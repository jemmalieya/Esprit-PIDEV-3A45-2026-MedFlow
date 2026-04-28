package tn.esprit.entities;

public class OpenAlexReference {

    private String title;
    private int year;
    private String source;
    private String authors;
    private String doi;
    private String url;
    private int citationCount;

    public OpenAlexReference() {
    }

    public OpenAlexReference(String title, int year, String source, String authors,
                             String doi, String url, int citationCount) {
        this.title = title;
        this.year = year;
        this.source = source;
        this.authors = authors;
        this.doi = doi;
        this.url = url;
        this.citationCount = citationCount;
    }

    public String getTitle() {
        return title;
    }

    public int getYear() {
        return year;
    }

    public String getSource() {
        return source;
    }

    public String getAuthors() {
        return authors;
    }

    public String getDoi() {
        return doi;
    }

    public String getUrl() {
        return url;
    }

    public int getCitationCount() {
        return citationCount;
    }

    public String toPostReferenceText() {
        StringBuilder sb = new StringBuilder();

        sb.append("\n\n📚 Référence scientifique :\n");
        sb.append("- Titre : ").append(title != null ? title : "Sans titre").append("\n");

        if (authors != null && !authors.isBlank()) {
            sb.append("- Auteurs : ").append(authors).append("\n");
        }

        if (year > 0) {
            sb.append("- Année : ").append(year).append("\n");
        }

        if (source != null && !source.isBlank()) {
            sb.append("- Source : ").append(source).append("\n");
        }

        sb.append("- Citations : ").append(citationCount).append("\n");

        if (url != null && !url.isBlank()) {
            sb.append("- Lien : ").append(url).append("\n");
        }

        return sb.toString();
    }
}