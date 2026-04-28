package tn.esprit.services;

import ai.onnxruntime.NodeInfo;
import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OnnxValue;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import ai.onnxruntime.TensorInfo;
import nu.pattern.OpenCV;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class HandTrackingService {

    public static final String DEBUG_BUILD_MARKER = "hand-tracking-build-2026-04-28-v2-fixed-affine";

    private static final int PALM_INPUT_SIZE = 192;
    private static final int HAND_INPUT_SIZE = 224;
    private static final double PALM_SCORE_THRESHOLD = 0.60;
    private static final double HAND_SCORE_THRESHOLD = 0.60;
    private static final double PINCH_DISTANCE_THRESHOLD = 0.045;
    private static final int[][] HAND_CONNECTIONS = {
            {0, 1}, {1, 2}, {2, 3}, {3, 4},
            {0, 5}, {5, 6}, {6, 7}, {7, 8},
            {5, 9}, {9, 10}, {10, 11}, {11, 12},
            {9, 13}, {13, 14}, {14, 15}, {15, 16},
            {13, 17}, {17, 18}, {18, 19}, {19, 20},
            {0, 17}
    };

    private final double[][] palmAnchors;
    private final OrtEnvironment environment;
    private final OrtSession palmSession;
    private final OrtSession handSession;
    private final String palmInputName;
    private final String handInputName;
    private final long[] palmInputShape;
    private final long[] handInputShape;
    private final List<String> palmOutputNames;
    private final List<String> handOutputNames;

    public HandTrackingService(Path palmModelPath, Path handPoseModelPath) throws IOException {
        ensureOpenCvLoaded();
        System.out.println("[HandTrackingService] " + DEBUG_BUILD_MARKER);

        if (!Files.exists(palmModelPath)) {
            throw new IOException("Palm detection model not found: " + palmModelPath);
        }
        if (!Files.exists(handPoseModelPath)) {
            throw new IOException("Hand pose model not found: " + handPoseModelPath);
        }

        try {
            environment = OrtEnvironment.getEnvironment();
            OrtSession.SessionOptions options = new OrtSession.SessionOptions();
            palmSession = environment.createSession(palmModelPath.toAbsolutePath().toString(), options);
            handSession = environment.createSession(handPoseModelPath.toAbsolutePath().toString(), options);

            palmInputName = firstInputName(palmSession);
            handInputName = firstInputName(handSession);
            palmInputShape = inputShape(palmSession, palmInputName);
            handInputShape = inputShape(handSession, handInputName);
            palmOutputNames = new ArrayList<>(palmSession.getOutputInfo().keySet());
            handOutputNames = new ArrayList<>(handSession.getOutputInfo().keySet());
            palmAnchors = buildPalmAnchors();
        } catch (OrtException ex) {
            throw new IOException("Unable to initialize ONNX Runtime sessions", ex);
        }
    }

    public HandTrackingResult track(BufferedImage frame) {
        if (frame == null) {
            return HandTrackingResult.empty(0, 0, "No frame");
        }

        Mat image = bufferedImageToMat(frame);
        try {
            PalmDetectionResult palmResult = detectPalm(image);
            if (palmResult.detection == null) {
                return HandTrackingResult.empty(frame.getWidth(), frame.getHeight(), palmResult.statusMessage);
            }

            HandTrackingResult result = detectHandLandmarks(image, palmResult.detection, frame.getWidth(), frame.getHeight());
            return result == null
                    ? HandTrackingResult.empty(frame.getWidth(), frame.getHeight(), "Hand landmarks unavailable")
                    : result;
        } finally {
            image.release();
        }
    }

    public int[][] getHandConnections() {
        return HAND_CONNECTIONS;
    }

    private static void ensureOpenCvLoaded() {
        OpenCV.loadLocally();
    }

    private PalmDetectionResult detectPalm(Mat image) {
        PreprocessedPalm preprocessed = preprocessPalm(image);
        try {
            ModelRunResult outputs = runModel(palmSession, palmInputName, palmInputShape, palmOutputNames, preprocessed.normalizedRgb);
            if (outputs.errorMessage != null) {
                return new PalmDetectionResult(null, "Palm inference error: " + outputs.errorMessage);
            }

            float[] boxAndLandmarks = null;
            float[] rawScores = null;
            for (float[] output : outputs.outputs) {
                if (output.length == 2016) {
                    rawScores = output;
                } else if (output.length == 2016 * 18) {
                    boxAndLandmarks = output;
                }
            }

            if (boxAndLandmarks == null || rawScores == null) {
                return new PalmDetectionResult(null, "Unable to map palm model outputs");
            }

            PalmDetection bestDetection = null;
            double bestRankingScore = Double.NEGATIVE_INFINITY;
            double bestScore = 0.0;
            double scale = Math.max(preprocessed.originalWidth, preprocessed.originalHeight);
            double minPalmArea = image.width() * image.height() * 0.010;

            for (int i = 0; i < rawScores.length; i++) {
                double score = sigmoid(rawScores[i]);
                if (score < PALM_SCORE_THRESHOLD) {
                    continue;
                }

                int offset = i * 18;
                double[] anchor = palmAnchors[i];

                double cx = boxAndLandmarks[offset] / PALM_INPUT_SIZE + anchor[0];
                double cy = boxAndLandmarks[offset + 1] / PALM_INPUT_SIZE + anchor[1];
                double w = Math.abs(boxAndLandmarks[offset + 2] / PALM_INPUT_SIZE);
                double h = Math.abs(boxAndLandmarks[offset + 3] / PALM_INPUT_SIZE);

                double x1 = (cx - w / 2.0) * scale - preprocessed.padBiasX;
                double y1 = (cy - h / 2.0) * scale - preprocessed.padBiasY;
                double x2 = (cx + w / 2.0) * scale - preprocessed.padBiasX;
                double y2 = (cy + h / 2.0) * scale - preprocessed.padBiasY;

                x1 = clampToRange(x1, 0, preprocessed.originalWidth - 1);
                y1 = clampToRange(y1, 0, preprocessed.originalHeight - 1);
                x2 = clampToRange(x2, 0, preprocessed.originalWidth - 1);
                y2 = clampToRange(y2, 0, preprocessed.originalHeight - 1);

                double width = Math.max(1.0, x2 - x1);
                double height = Math.max(1.0, y2 - y1);
                double area = width * height;

                if (area < minPalmArea) {
                    continue;
                }

                double[] palmData = new double[19];
                palmData[0] = x1;
                palmData[1] = y1;
                palmData[2] = x2;
                palmData[3] = y2;

                for (int k = 0; k < 7; k++) {
                    double lx = (boxAndLandmarks[offset + 4 + k * 2] / PALM_INPUT_SIZE + anchor[0]) * scale - preprocessed.padBiasX;
                    double ly = (boxAndLandmarks[offset + 5 + k * 2] / PALM_INPUT_SIZE + anchor[1]) * scale - preprocessed.padBiasY;
                    palmData[4 + k * 2] = clampToRange(lx, 0, preprocessed.originalWidth - 1);
                    palmData[5 + k * 2] = clampToRange(ly, 0, preprocessed.originalHeight - 1);
                }
                palmData[18] = score;

                double rankingScore = score + 0.35 * Math.min(1.0, area / (image.width() * image.height() * 0.10));
                if (rankingScore > bestRankingScore) {
                    bestRankingScore = rankingScore;
                    bestScore = score;
                    bestDetection = new PalmDetection(palmData);
                }
            }

            if (bestDetection == null) {
                return new PalmDetectionResult(null, "Palm not detected or rejected as false positive");
            }

            return new PalmDetectionResult(bestDetection, String.format("Palm detected. Score %.3f", bestScore));
        } finally {
            preprocessed.normalizedRgb.release();
        }
    }



    private HandTrackingResult detectHandLandmarks(Mat image, PalmDetection palm, int imageWidth, int imageHeight) {
        HandPreprocessResult preprocess = preprocessHand(image, palm.values);
        Mat inverseRotationMatrix = new Mat();
        try {
            ModelRunResult outputs = runModel(handSession, handInputName, handInputShape, handOutputNames, preprocess.normalizedRgb);
            if (outputs.errorMessage != null) {
                return HandTrackingResult.empty(imageWidth, imageHeight, "Hand inference error: " + outputs.errorMessage);
            }

            float[] landmarksOutput = null;
            float[] confidenceOutput = null;
            float[] handednessOutput = null;
            float[] worldOutput = null;
            int sixtyThreeCount = 0;
            for (float[] output : outputs.outputs) {
                if (output.length == 63) {
                    if (sixtyThreeCount == 0) {
                        landmarksOutput = output;
                    } else {
                        worldOutput = output;
                    }
                    sixtyThreeCount++;
                } else if (output.length == 1) {
                    if (confidenceOutput == null) {
                        confidenceOutput = output;
                    } else {
                        handednessOutput = output;
                    }
                }
            }

            if (landmarksOutput == null || worldOutput == null || confidenceOutput == null || handednessOutput == null) {
                return HandTrackingResult.empty(imageWidth, imageHeight, "Unable to map hand model outputs");
            }

            double confidence = confidenceOutput[0];
            if (confidence < HAND_SCORE_THRESHOLD) {
                return HandTrackingResult.empty(imageWidth, imageHeight, String.format("Palm found, hand confidence too low: %.3f", confidence));
            }

            Imgproc.invertAffineTransform(preprocess.rotationMatrix, inverseRotationMatrix);

            double modelToCropScale = preprocess.finalCropSideLength / (double) HAND_INPUT_SIZE;
            List<NormalizedLandmark> landmarks = new ArrayList<>(21);

            for (int i = 0; i < 21; i++) {
                double cropPaddedX = landmarksOutput[i * 3] * modelToCropScale;
                double cropPaddedY = landmarksOutput[i * 3 + 1] * modelToCropScale;
                double z = landmarksOutput[i * 3 + 2] * modelToCropScale;

                double rotatedImageX = cropPaddedX + preprocess.finalCropBiasX;
                double rotatedImageY = cropPaddedY + preprocess.finalCropBiasY;

                double localX = rotatedImageX * inverseRotationMatrix.get(0, 0)[0]
                        + rotatedImageY * inverseRotationMatrix.get(0, 1)[0]
                        + inverseRotationMatrix.get(0, 2)[0];
                double localY = rotatedImageX * inverseRotationMatrix.get(1, 0)[0]
                        + rotatedImageY * inverseRotationMatrix.get(1, 1)[0]
                        + inverseRotationMatrix.get(1, 2)[0];

                double imageX = localX + preprocess.initialCropBiasX;
                double imageY = localY + preprocess.initialCropBiasY;
                double normX = imageX / imageWidth;
                double normY = imageY / imageHeight;



                landmarks.add(new NormalizedLandmark(
                        clamp(normX),
                        clamp(normY),
                        z
                ));
            }

            NormalizedLandmark thumbTip = landmarks.get(4);
            NormalizedLandmark indexTip = landmarks.get(8);
            boolean pinchActive = distance(thumbTip, indexTip) < PINCH_DISTANCE_THRESHOLD;

            return new HandTrackingResult(
                    true,
                    landmarks,
                    pinchActive,
                    indexTip,
                    confidence,
                    handednessOutput[0],
                    pinchActive ? "Pinch detected" : String.format("Hand detected. Confidence %.3f", confidence),
                    imageWidth,
                    imageHeight
            );
        } finally {
            preprocess.normalizedRgb.release();
            preprocess.rotationMatrix.release();
            inverseRotationMatrix.release();
        }
    }

    private ModelRunResult runModel(
            OrtSession session,
            String inputName,
            long[] modelShape,
            List<String> outputNames,
            Mat normalizedRgb
    ) {
        TensorInput tensorInput = tensorInputFromMat(normalizedRgb, modelShape);
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(tensorInput.data.length * Float.BYTES).order(ByteOrder.nativeOrder());
        FloatBuffer floatBuffer = byteBuffer.asFloatBuffer();
        floatBuffer.put(tensorInput.data);
        floatBuffer.rewind();

        try (OnnxTensor inputTensor = OnnxTensor.createTensor(environment, floatBuffer, tensorInput.shape);
             OrtSession.Result result = session.run(Collections.singletonMap(inputName, inputTensor))) {
            List<float[]> outputs = new ArrayList<>();
            for (String outputName : outputNames) {
                OnnxValue value = result.get(outputName).orElse(null);
                if (value == null) {
                    continue;
                }
                outputs.add(flattenOnnxValue(value));
            }
            return new ModelRunResult(outputs, null);
        } catch (OrtException ex) {
            return new ModelRunResult(Collections.emptyList(), ex.getClass().getSimpleName() + ": " + ex.getMessage());
        }
    }

    private PreprocessedPalm preprocessPalm(Mat image) {
        int originalHeight = image.rows();
        int originalWidth = image.cols();

        double ratio = Math.min((double) PALM_INPUT_SIZE / originalHeight, (double) PALM_INPUT_SIZE / originalWidth);
        int resizedHeight = Math.max(1, (int) Math.round(originalHeight * ratio));
        int resizedWidth = Math.max(1, (int) Math.round(originalWidth * ratio));

        Mat resized = new Mat();
        Imgproc.resize(image, resized, new Size(resizedWidth, resizedHeight));

        int padTop = (PALM_INPUT_SIZE - resizedHeight) / 2;
        int padBottom = PALM_INPUT_SIZE - resizedHeight - padTop;
        int padLeft = (PALM_INPUT_SIZE - resizedWidth) / 2;
        int padRight = PALM_INPUT_SIZE - resizedWidth - padLeft;

        Mat padded = new Mat();
        Core.copyMakeBorder(resized, padded, padTop, padBottom, padLeft, padRight, Core.BORDER_CONSTANT, new Scalar(0, 0, 0));
        resized.release();

        Mat rgb = new Mat();
        Imgproc.cvtColor(padded, rgb, Imgproc.COLOR_BGR2RGB);
        padded.release();
        rgb.convertTo(rgb, CvType.CV_32F, 1.0 / 255.0);

        return new PreprocessedPalm(rgb, (int) Math.round(padLeft / ratio), (int) Math.round(padTop / ratio), originalWidth, originalHeight);
    }

    private HandPreprocessResult preprocessHand(Mat image, double[] palm) {
        double[][] palmBox = {
                {palm[0], palm[1]},
                {palm[2], palm[3]}
        };

        CropResult initialCrop = cropAndPadFromPalm(image, palmBox, true);
        Mat rgb = new Mat();
        Imgproc.cvtColor(initialCrop.image, rgb, Imgproc.COLOR_BGR2RGB);
        initialCrop.image.release();

        double initialCropBiasX = initialCrop.biasX;
        double initialCropBiasY = initialCrop.biasY;

        double[][] palmLandmarks = new double[7][2];
        for (int i = 0; i < 7; i++) {
            palmLandmarks[i][0] = palm[4 + i * 2] - initialCropBiasX;
            palmLandmarks[i][1] = palm[5 + i * 2] - initialCropBiasY;
        }

        double[][] localPalmBox = {
                {initialCrop.box[0][0] - initialCropBiasX, initialCrop.box[0][1] - initialCropBiasY},
                {initialCrop.box[1][0] - initialCropBiasX, initialCrop.box[1][1] - initialCropBiasY}
        };

        double[] p1 = palmLandmarks[0];
        double[] p2 = palmLandmarks[2];
        double radians = Math.PI / 2.0 - Math.atan2(-(p2[1] - p1[1]), p2[0] - p1[0]);
        radians = radians - 2.0 * Math.PI * Math.floor((radians + Math.PI) / (2.0 * Math.PI));
        double angle = Math.toDegrees(radians);

        Point centerPalmBox = new Point(
                (localPalmBox[0][0] + localPalmBox[1][0]) / 2.0,
                (localPalmBox[0][1] + localPalmBox[1][1]) / 2.0
        );
        Mat rotationMatrix = Imgproc.getRotationMatrix2D(centerPalmBox, angle, 1.0);
        Mat rotatedImage = new Mat();
        Imgproc.warpAffine(rgb, rotatedImage, rotationMatrix, rgb.size());
        rgb.release();

        double[][] rotatedPalmLandmarks = new double[7][2];
        for (int i = 0; i < palmLandmarks.length; i++) {
            double x = palmLandmarks[i][0];
            double y = palmLandmarks[i][1];
            rotatedPalmLandmarks[i][0] = x * rotationMatrix.get(0, 0)[0] + y * rotationMatrix.get(0, 1)[0] + rotationMatrix.get(0, 2)[0];
            rotatedPalmLandmarks[i][1] = x * rotationMatrix.get(1, 0)[0] + y * rotationMatrix.get(1, 1)[0] + rotationMatrix.get(1, 2)[0];
        }

        double[][] rotatedPalmBox = {
                {minCoord(rotatedPalmLandmarks, 0), minCoord(rotatedPalmLandmarks, 1)},
                {maxCoord(rotatedPalmLandmarks, 0), maxCoord(rotatedPalmLandmarks, 1)}
        };

        CropResult finalCrop = cropAndPadFromPalm(rotatedImage, rotatedPalmBox, false);
        rotatedImage.release();

        int finalCropSideLength = finalCrop.image.cols();
        Mat resized = new Mat();
        Imgproc.resize(finalCrop.image, resized, new Size(HAND_INPUT_SIZE, HAND_INPUT_SIZE), 0, 0, Imgproc.INTER_AREA);
        finalCrop.image.release();
        resized.convertTo(resized, CvType.CV_32F, 1.0 / 255.0);

        return new HandPreprocessResult(
                resized,
                rotationMatrix,
                initialCropBiasX,
                initialCropBiasY,
                finalCrop.biasX,
                finalCrop.biasY,
                finalCropSideLength
        );
    }

    private CropResult cropAndPadFromPalm(Mat image, double[][] palmBox, boolean forRotation) {
        double[] shiftedTopLeft = palmBox[0].clone();
        double[] shiftedBottomRight = palmBox[1].clone();
        double width = shiftedBottomRight[0] - shiftedTopLeft[0];
        double height = shiftedBottomRight[1] - shiftedTopLeft[1];

        double shiftY = forRotation ? 0.0 : -0.4 * height;
        shiftedTopLeft[1] += shiftY;
        shiftedBottomRight[1] += shiftY;

        double centerX = (shiftedTopLeft[0] + shiftedBottomRight[0]) / 2.0;
        double centerY = (shiftedTopLeft[1] + shiftedBottomRight[1]) / 2.0;
        double enlargeScale = forRotation ? 2.4 : 2.1;
        double halfWidth = width * enlargeScale / 2.0;
        double halfHeight = height * enlargeScale / 2.0;

        int x1 = (int) Math.max(0, Math.floor(centerX - halfWidth));
        int y1 = (int) Math.max(0, Math.floor(centerY - halfHeight));
        int x2 = (int) Math.min(image.cols(), Math.ceil(centerX + halfWidth));
        int y2 = (int) Math.min(image.rows(), Math.ceil(centerY + halfHeight));

        if (x2 <= x1 || y2 <= y1) {
            x1 = 0;
            y1 = 0;
            x2 = image.cols();
            y2 = image.rows();
        }

        Rect roi = new Rect(x1, y1, Math.max(1, x2 - x1), Math.max(1, y2 - y1));
        Mat cropped = new Mat(image, roi).clone();

        double diagonal = Math.sqrt(cropped.rows() * (double) cropped.rows() + cropped.cols() * (double) cropped.cols());
        int sideLength = forRotation ? (int) Math.ceil(diagonal) : Math.max(cropped.rows(), cropped.cols());

        int padHeight = sideLength - cropped.rows();
        int padWidth = sideLength - cropped.cols();
        int left = padWidth / 2;
        int top = padHeight / 2;
        int right = padWidth - left;
        int bottom = padHeight - top;

        Mat padded = new Mat();
        Core.copyMakeBorder(cropped, padded, top, bottom, left, right, Core.BORDER_CONSTANT, new Scalar(0, 0, 0));
        cropped.release();

        double[][] adjustedBox = {
                {x1, y1},
                {x2, y2}
        };
        return new CropResult(padded, adjustedBox, x1 - left, y1 - top);
    }

    private TensorInput tensorInputFromMat(Mat normalizedRgb, long[] modelShape) {
        int rows = normalizedRgb.rows();
        int cols = normalizedRgb.cols();
        int channels = normalizedRgb.channels();
        long[] resolvedShape = resolveShape(modelShape, rows, cols, channels);

        if (resolvedShape.length != 4) {
            throw new IllegalArgumentException("Expected 4D model input, got " + resolvedShape.length + "D");
        }

        boolean nchw = resolvedShape[1] == channels;
        if (nchw) {
            List<Mat> splitChannels = new ArrayList<>(channels);
            Core.split(normalizedRgb, splitChannels);
            int planeSize = rows * cols;
            float[] data = new float[channels * planeSize];
            for (int c = 0; c < channels; c++) {
                float[] channelData = new float[planeSize];
                splitChannels.get(c).get(0, 0, channelData);
                System.arraycopy(channelData, 0, data, c * planeSize, planeSize);
            }
            releaseAll(splitChannels);
            return new TensorInput(data, resolvedShape);
        }

        float[] data = new float[rows * cols * channels];
        normalizedRgb.get(0, 0, data);
        return new TensorInput(data, resolvedShape);
    }

    private static long[] resolveShape(long[] modelShape, int rows, int cols, int channels) {
        long[] resolved = modelShape.clone();
        if (resolved.length != 4) {
            return resolved;
        }

        if (resolved[0] <= 0) {
            resolved[0] = 1;
        }

        if (resolved[1] == channels || resolved[1] == -1 || resolved[1] == 0) {
            if (resolved[1] == -1 || resolved[1] == 0) {
                resolved[1] = channels;
                resolved[2] = rows;
                resolved[3] = cols;
            }
            return resolved;
        }

        if (resolved[3] == channels || resolved[3] == -1 || resolved[3] == 0) {
            if (resolved[1] <= 0) {
                resolved[1] = rows;
            }
            if (resolved[2] <= 0) {
                resolved[2] = cols;
            }
            resolved[3] = channels;
            return resolved;
        }

        resolved[1] = channels;
        resolved[2] = rows;
        resolved[3] = cols;
        return resolved;
    }

    private static float[] flattenOnnxValue(OnnxValue value) throws OrtException {
        if (!(value instanceof OnnxTensor tensor)) {
            return new float[0];
        }
        Object raw = tensor.getValue();
        List<Float> values = new ArrayList<>();
        flattenRecursive(raw, values);
        float[] result = new float[values.size()];
        for (int i = 0; i < values.size(); i++) {
            result[i] = values.get(i);
        }
        return result;
    }

    private static void flattenRecursive(Object raw, List<Float> out) {
        if (raw == null) {
            return;
        }
        if (raw instanceof float[] floats) {
            for (float value : floats) {
                out.add(value);
            }
            return;
        }
        if (raw instanceof double[] doubles) {
            for (double value : doubles) {
                out.add((float) value);
            }
            return;
        }
        if (!raw.getClass().isArray()) {
            return;
        }
        int length = Array.getLength(raw);
        for (int i = 0; i < length; i++) {
            flattenRecursive(Array.get(raw, i), out);
        }
    }

    private static String firstInputName(OrtSession session) throws IOException {
        try {
            return session.getInputInfo().keySet().iterator().next();
        } catch (OrtException ex) {
            throw new IOException("Unable to inspect ONNX input names", ex);
        }
    }

    private static long[] inputShape(OrtSession session, String inputName) throws IOException {
        try {
            NodeInfo nodeInfo = session.getInputInfo().get(inputName);
            TensorInfo tensorInfo = (TensorInfo) nodeInfo.getInfo();
            return tensorInfo.getShape();
        } catch (OrtException ex) {
            throw new IOException("Unable to inspect ONNX input shape for " + inputName, ex);
        }
    }

    private static Mat bufferedImageToMat(BufferedImage image) {
        BufferedImage converted = image;
        if (image.getType() != BufferedImage.TYPE_3BYTE_BGR) {
            converted = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
            converted.getGraphics().drawImage(image, 0, 0, null);
        }

        byte[] pixels = ((DataBufferByte) converted.getRaster().getDataBuffer()).getData();
        Mat mat = new Mat(converted.getHeight(), converted.getWidth(), CvType.CV_8UC3);
        mat.put(0, 0, pixels);
        return mat;
    }

    private static double[][] buildPalmAnchors() {
        List<double[]> anchors = new ArrayList<>(2016);
        addAnchorGrid(anchors, 24, 2);
        addAnchorGrid(anchors, 12, 6);
        return anchors.toArray(new double[0][]);
    }

    private static void addAnchorGrid(List<double[]> anchors, int gridSize, int repeatsPerCell) {
        double step = 1.0 / gridSize;
        for (int y = 0; y < gridSize; y++) {
            for (int x = 0; x < gridSize; x++) {
                double centerX = (x + 0.5) * step;
                double centerY = (y + 0.5) * step;
                for (int i = 0; i < repeatsPerCell; i++) {
                    anchors.add(new double[]{centerX, centerY});
                }
            }
        }
    }

    private static double sigmoid(float value) {
        return 1.0 / (1.0 + Math.exp(-value));
    }

    private static double minCoord(double[][] points, int axis) {
        double min = Double.POSITIVE_INFINITY;
        for (double[] point : points) {
            min = Math.min(min, point[axis]);
        }
        return min;
    }

    private static double maxCoord(double[][] points, int axis) {
        double max = Double.NEGATIVE_INFINITY;
        for (double[] point : points) {
            max = Math.max(max, point[axis]);
        }
        return max;
    }

    private static void releaseAll(List<Mat> mats) {
        for (Mat mat : mats) {
            mat.release();
        }
    }

    private static double dot(double[] a, double[] b) {
        return a[0] * b[0] + a[1] * b[1];
    }

    private static double distance(NormalizedLandmark a, NormalizedLandmark b) {
        double dx = a.x() - b.x();
        double dy = a.y() - b.y();
        return Math.sqrt(dx * dx + dy * dy);
    }

    private static double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
    private static double clampToRange(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private record TensorInput(float[] data, long[] shape) {
    }

    private static class ModelRunResult {
        private final List<float[]> outputs;
        private final String errorMessage;

        private ModelRunResult(List<float[]> outputs, String errorMessage) {
            this.outputs = outputs;
            this.errorMessage = errorMessage;
        }
    }

    private static class PreprocessedPalm {
        private final Mat normalizedRgb;
        private final int padBiasX;
        private final int padBiasY;
        private final int originalWidth;
        private final int originalHeight;

        private PreprocessedPalm(Mat normalizedRgb, int padBiasX, int padBiasY, int originalWidth, int originalHeight) {
            this.normalizedRgb = normalizedRgb;
            this.padBiasX = padBiasX;
            this.padBiasY = padBiasY;
            this.originalWidth = originalWidth;
            this.originalHeight = originalHeight;
        }
    }

    private static class CropResult {
        private final Mat image;
        private final double[][] box;
        private final int biasX;
        private final int biasY;

        private CropResult(Mat image, double[][] box, int biasX, int biasY) {
            this.image = image;
            this.box = box;
            this.biasX = biasX;
            this.biasY = biasY;
        }
    }

    private static class HandPreprocessResult {
        private final Mat normalizedRgb;
        private final Mat rotationMatrix;
        private final double initialCropBiasX;
        private final double initialCropBiasY;
        private final double finalCropBiasX;
        private final double finalCropBiasY;
        private final int finalCropSideLength;

        private HandPreprocessResult(
                Mat normalizedRgb,
                Mat rotationMatrix,
                double initialCropBiasX,
                double initialCropBiasY,
                double finalCropBiasX,
                double finalCropBiasY,
                int finalCropSideLength
        ) {
            this.normalizedRgb = normalizedRgb;
            this.rotationMatrix = rotationMatrix;
            this.initialCropBiasX = initialCropBiasX;
            this.initialCropBiasY = initialCropBiasY;
            this.finalCropBiasX = finalCropBiasX;
            this.finalCropBiasY = finalCropBiasY;
            this.finalCropSideLength = finalCropSideLength;
        }
    }

    private static class PalmDetection {
        private final double[] values;

        private PalmDetection(double[] values) {
            this.values = values;
        }
    }

    private static class PalmDetectionResult {
        private final PalmDetection detection;
        private final String statusMessage;

        private PalmDetectionResult(PalmDetection detection, String statusMessage) {
            this.detection = detection;
            this.statusMessage = statusMessage;
        }
    }

    public record NormalizedLandmark(double x, double y, double z) {
    }

    public record HandTrackingResult(
            boolean handDetected,
            List<NormalizedLandmark> landmarks,
            boolean pinchActive,
            NormalizedLandmark penTip,
            double confidence,
            double handedness,
            String statusMessage,
            int imageWidth,
            int imageHeight
    ) {
        public static HandTrackingResult empty(int imageWidth, int imageHeight, String statusMessage) {
            return new HandTrackingResult(
                    false,
                    Collections.emptyList(),
                    false,
                    null,
                    0.0,
                    0.0,
                    statusMessage,
                    imageWidth,
                    imageHeight
            );
        }
    }
}
