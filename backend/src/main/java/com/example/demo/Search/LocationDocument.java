package com.example.demo.Search;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.InnerField;
import org.springframework.data.elasticsearch.annotations.MultiField;
import org.springframework.data.elasticsearch.annotations.Setting;

/**
 * UES: Elasticsearch dokument za pretragu mesta.
 * Indeks "locations" se kreira sa custom analyzer-om {@code sr_custom}
 * (ćirilica->latinica + lowercase + asciifolding) definisanim u es/location-settings.json.
 */
@Document(indexName = "locations")
@Setting(settingPath = "es/location-settings.json")
@Getter
@Setter
@NoArgsConstructor
public class LocationDocument {

    @Id
    private String id;

    // naziv: analizirano polje za pretragu + .keyword sub-polje za sortiranje (ocena 10)
    @MultiField(
            mainField = @Field(type = FieldType.Text, analyzer = "sr_custom"),
            otherFields = {@InnerField(suffix = "keyword", type = FieldType.Keyword)}
    )
    private String naziv;

    @Field(type = FieldType.Text, analyzer = "sr_custom")
    private String opis;

    // pdfOpis: parsiran tekst iz PDF-a (ocena 6)
    @Field(type = FieldType.Text, analyzer = "sr_custom")
    private String pdfOpis;

    @Field(type = FieldType.Keyword)
    private String tipMesta;

    @Field(type = FieldType.Text, analyzer = "sr_custom")
    private String adresa;

    // reviewCount: za pretragu po opsegu broja review-a (ocena 7)
    @Field(type = FieldType.Integer)
    private Integer reviewCount;

    // prosečne ocene po kategorijama: za pretragu po opsegu (ocena 10)
    @Field(type = FieldType.Double)
    private Double avgNastup;

    @Field(type = FieldType.Double)
    private Double avgZvukSvetlo;

    @Field(type = FieldType.Double)
    private Double avgProstor;

    @Field(type = FieldType.Double)
    private Double avgUkupno;

    @Field(type = FieldType.Double)
    private Double prosecnaOcena;

    // imageId: da frontend može da prikaže sliku rezultata preko /api/images/{id}
    @Field(type = FieldType.Long)
    private Long imageId;

    @Field(type = FieldType.Boolean)
    private Boolean hasPdf;
}
