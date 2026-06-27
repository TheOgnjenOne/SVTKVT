# UES 2026 — Šta je preostalo da se uradi (implementacioni spec)

> Generisano poređenjem `UES-2026.pdf` (zvanična specifikacija) sa stanjem koda u `backend/` i `frontend/`.
> Cilj: lista svega što NIJE gotovo, dovoljno detaljna da se apsolutno sve dovrši.

---

## 1. Rezime stanja

| Deo projekta | Predmet | Status |
|---|---|---|
| **SVT / KVA** (Spring + Angular + MySQL, K1–K10, M1–M4, A1–A2) | SVT/KVA | ✅ ~95% gotovo (par sitnih ispravki — vidi §4) |
| **UES DEO** (Elasticsearch + MinIO + PDF full-text pretraga, S1) | Napredne baze (UES) | ❌ **0% — nije ni započet** |

**Zaključak:** Glavni posao koji preostaje je **ceo UES deo**. U `pom.xml` nema nijedne zavisnosti za Elasticsearch, MinIO, Tika/PDFBox; u `application.properties` nema nikakvog ES/MinIO configa; nema nijedne klase (`*Document`, `*SearchRepository`, search controller/service). Slike se trenutno čuvaju na fajl-sistemu (`Image.path`, `app.upload.dir=uploads`), ne u MinIO.

> ⚠️ **Napomena o oceni:** U PDF-u su zahtevi obojeni po ocenama (žuto=6, crveno=7, zeleno=8, roze=9, plavo=10). Izvukao sam to mapiranje iz PDF highlight-a — vidi §3. **UES deo ima sopstvenu skalu 6→10**: nije „sve ili ništa", svaki dodatni tip pretrage diže ocenu.

---

## 2. Šta JE već urađeno (da se zna na šta se naslanjamo)

SVT/KVA backend (`com.example.demo`) ima kompletno:

- **Auth/JWT**: `JwtAuth/JwtUtil.java`, `SecurityConfig.java`, `JwtRequestFilter.java`; login/logout, `@PreAuthorize` role-based pristup. (K1, K2, A1, auth) ✅
- **Mesta**: `Model/Location.java`, `LocationController`, `LocationServiceImpl` — CRUD, admin-only kreiranje/brisanje, menadžer ažurira atribute, prosečna ocena, predstojeći događaji. (K3, M1, A2) ✅
- **Događaji**: `Model/Event.java`, `EventController` — redovan/cena/besplatan, slika. (K4) ✅
- **Recenzije**: `Model/Review.java`, `ReviewController` — logičko brisanje (`deleted`), skrivanje (`isHidden`), provera „redovan + već održan", `eventCount`. (K5, M2) ✅
- **Komentari (reply tree)**: `Model/Comment.java` (self-reference `parentComment`), `comment-tree` Angular komponenta — proizvoljna dubina. (M3) ✅
- **Analitika**: `AnalyticsController`, `LocationAnalyticsServiceImpl`; frontend `location-analytics` koristi **Chart.js + ng2-charts** (potvrđeno u `frontend/package.json`). (M4) ✅
- **Profil / lozinka / email**: `UserController`, `UserServiceImpl.changePassword`, `EmailService` (Mailtrap). (K9, K10) ✅
- **Pretraga/filtriranje i sortiranje (relaciona, ne-ES)**: `location-page.ts`, `event-page.ts`, `all-reviews-modal.ts`. (K6, K7, K8) ✅
- **Logovanje**: SLF4J kroz kontrolere/servise. ✅

Frontend (Angular standalone) ima sve odgovarajuće stranice (auth, dashboard, location, event, profile, admin, analytics).

---

## 3. Mapiranje zahteva na ocene (iz PDF highlight-a)

### SVT/KVA (informativno — uglavnom gotovo)
| Ocena | Zahtevi |
|---|---|
| 6 (žuta) | K3 baza (obavezna polja, prosečna ocena), K4 baza, K5 (ocenjivanje 1–10, opciono), K6 baza |
| 7 (crvena) | K1, K2, K9 (unos lozinki), K10 (dodatni podaci), auth tokenom |
| 8 (zelena) | K3 (admin/menadžer prava), K4 (samo menadžer), K5 (opc. komentar + restrikcija redovan/održan), K10 (spisak utisaka+mesta), M1, M2, M3 (reply), A2, logovanje |
| 9 (roze) | K3 (mesto mora sliku, predstojeći događaji), K4 (slika), K8 (početna + najpopularnija), K10 (slika profila) |
| 10 (plava) | K6 (proizvoljan datum), K7 (sortiranje utisaka), M3 (proizvoljna dubina reply), M4 (analitika+grafici), K9/A1 (slanje mejla) |

### UES DEO (ovo je ono što treba uraditi — skala 6→10)
| Ocena | UES zahtev |
|---|---|
| **6 (žuta)** | Indeksiranje mesta u ES + čuvanje slika/dokumenata u **MinIO** + upload **PDF** opisa; pretraga po **nazivu**; pretraga po **opisu**; pretraga po **opisu iz PDF-a** (parsiran, indeksiran kao Text); **custom analyzer** (case-insensitive + ćirilica/latinica, NE samo ugrađeni Serbian); prikaz rezultata (naziv + opis iz UI-a); **download PDF-a** iz prikaza mesta |
| **7 (crvena)** | Pretraga po **opsegu broja review-a** (od–do, donja i/ili gornja granica) |
| **8 (zelena)** | **Kombinacija** parametara — BooleanQuery sa **AND i OR** operatorom između polja |
| **9 (roze)** | Podrška za **PhraseQuery** (`"..."`), **PrefixQuery** (`Ivan M*`), **FuzzyQuery** (`~rizika`) iz polja forme; **MoreLikeThis** (polja: naslov, opis, opis iz PDF-a) |
| **10 (plava)** | Pretraga po **opsegu prosečne ocene po kategorijama** (nastup, zvuk, svetlo, prostor, ukupan utisak); **sortiranje rezultata po nazivu** (dopuna indeksne strukture — `keyword` sub-field); **Highlighter** (dinamički sažetak) |

---

## 4. Sitne ispravke u SVT/KVA delu (BUG — treba popraviti)

Ovo su jedine stvarne rupe van UES dela. Sve su sigurne za izmenu jer NE diraju model baze (Napomena 1 iz PDF-a) — menjaju se samo validacije/DTO.

- [ ] **K5 — skala ocena je 1–5 umesto 1–10 (polomljeno na granici).**
  `backend/.../DTOs/ReviewDTOs/ReviewRequestDTO.java` linije 16–32 imaju `@Max(value = 5)`, a frontend (`location-review.ts` `ratingOptions` → 1..10) šalje vrednosti do 10. Recenzija sa ocenom 6–10 **biva odbijena validacijom**. → Promeniti sva 4 `@Max(5)` u `@Max(10)` (i poruke).
- [ ] **K5 — ocene su obavezne, a spec kaže „nije neophodno oceniti svaku stavku".**
  Sva 4 polja su `@NotNull`. Frontend već dozvoljava da bar jedna bude uneta. → Skinuti `@NotNull` sa 4 rating polja (ostaviti validaciju „bar jedna" — već postoji na frontu, dodati i na backu po želji).
- [ ] **K5 — `ReviewResponseDTO` vraća samo `overallRating`.**
  Pojedinačne ocene (`performanceRating`, `soundLightingRating`, `venueRating`) postoje u `Model/Review.java` ali se ne vraćaju. → Dodati ih u `ReviewResponseDTO` i u mapper `ReviewServiceImpl.convertToReviewResponseDTO`. **Obavezno za UES ocenu 10** (pretraga po kategorijama traži ove vrednosti u ES indeksu).

> Napomena o kategorijama ocena: PDF je kontradiktoran — K5 navodi 4 kategorije („zvuk **i** svetlo" zajedno), a UES/S1 navodi 5 („zvuk", „svetlo" odvojeno). Model u kodu ima 4 kolone (`performance`, `soundLighting`, `venue`, `overall`). **Ostajemo na 4 polja** (ne dirati model baze) i UES pretragu po oceni radimo nad ta 4 polja, gde „zvuk i svetlo" ostaje jedna kategorija.

---

## 5. UES DEO — detaljan implementacioni plan

### 5.0 Infrastruktura (Docker)
- [ ] Dodati `docker-compose.yml` u root sa servisima:
  - **Elasticsearch** 8.x (single-node, `xpack.security.enabled=false` za dev, port 9200)
  - **Kibana** (opciono, za debug)
  - **MinIO** (port 9000 API + 9001 konzola, default kredencijali `minioadmin/minioadmin`)
- [ ] (Opciono) ostaviti MySQL u istom compose-u radi lakšeg pokretanja.

### 5.1 Zavisnosti (`backend/pom.xml`)
- [ ] `spring-boot-starter-data-elasticsearch` (Spring Data ES; verzija prati Boot 3.5.x → ES klijent 8.x)
- [ ] `io.minio:minio` (MinIO Java SDK, npr. 8.5.x)
- [ ] Parser za PDF tekst — jedno od:
  - `org.apache.pdfbox:pdfbox` (lakše, dovoljno za tekstualni PDF), ili
  - `org.apache.tika:tika-core` + `tika-parsers-standard-package` (robusnije, podržava i scan/Office)

### 5.2 Konfiguracija (`application.properties` / `application.yml`)
- [ ] ES: `spring.elasticsearch.uris=http://localhost:9200`
- [ ] MinIO: custom property-ji `minio.url`, `minio.access-key`, `minio.secret-key`, `minio.bucket.images`, `minio.bucket.pdfs` + `@ConfigurationProperties` ili `@Value`.
- [ ] `@Configuration` klasa `MinioConfig` koja pravi `MinioClient` bean i pri startu kreira bucket-e ako ne postoje (`bucketExists` / `makeBucket`).

### 5.3 MinIO sloj (ocena 6)
- [ ] `Services/IFileStorageService` + `Impl/MinioStorageService`:
  - `String upload(MultipartFile file, String bucket)` → vraća object key (npr. UUID + ext)
  - `InputStream/byte[] download(String bucket, String key)`
  - `void delete(String bucket, String key)`
  - `String presignedUrl(...)` (opciono, za direktan prikaz slike)
- [ ] **Migracija slika u MinIO:** trenutno `Model/Image.java` ima samo `path` (string, fajl-sistem). Najmanji zahvat: `path` postaje **MinIO object key**, a `ImageController`/`ImageServiceImpl` čita iz MinIO umesto sa diska. (Ovo NE menja šemu baze — kolona ostaje `path` string.)
- [ ] **PDF po mestu:** treba mesto da nosi referencu na PDF u MinIO.
  - Odluka: dodati polje `pdfKey` (String, nullable) na `Location` **ILI** novu malu tabelu/entitet `LocationDocument`. Dodavanje nullable kolone je minimalna izmena; spec eksplicitno traži PDF po mestu pa je opravdano. (Ako se strogo poštuje „ne menjati model baze", PDF key čuvati samo u ES dokumentu + zaseban MinIO objekat imenovan po `locationId`.)
  - Endpoint za upload PDF-a uz mesto i endpoint za **download PDF-a** (ocena 6).

### 5.4 Elasticsearch dokument i indeks (ocena 6 + 10)
- [ ] `search/LocationDocument.java` (`@Document(indexName = "locations")`):
  - `id`
  - `naziv` (`@Field(type = Text, analyzer = "sr_custom")` + sub-field `keyword` za **sortiranje po nazivu** — ocena 10)
  - `opis` (Text, custom analyzer) — opis iz UI-a
  - `pdfOpis` (Text, custom analyzer) — parsiran tekst iz PDF-a
  - `tipMesta`, `adresa`
  - `reviewCount` (Integer) — za opseg broja review-a (ocena 7)
  - `avgNastup`, `avgZvukSvetlo`, `avgProstor`, `avgUkupno` (Double/Float) — za opseg po kategorijama (ocena 10)
  - (opciono) `prosecnaOcena` ukupna
- [ ] **Custom analyzer** (ocena 6) — kreirati index sa settings koji sadrži:
  - `lowercase` filter (case-insensitivnost)
  - **ICU transliteraciju / mapping char_filter ćirilica↔latinica** (npr. `icu_transform` `Serbian-Latin/BGN` ili eksplicitni `mapping` char_filter ćir→lat), tako da upit „Београд" i „Beograd" daju isti rezultat
  - opc. `asciifolding`/serbian stemmer
  - Definisati settings preko `@Setting(settingPath = "es/location-settings.json")` ili `IndexOperations.create(settings, mappings)` pri startu.
  - ⚠️ Spec eksplicitno traži **sopstvenu konfiguraciju analyzer-a** — nije dovoljno samo ugrađeni `serbian` analyzer.
- [ ] Servis za (re)indeksiranje: pri kreiranju/izmeni mesta i pri dodavanju review-a osvežiti ES dokument (preračunati `reviewCount` i prosečne ocene po kategorijama). Inicijalni „reindex all" endpoint za seed.

### 5.5 Search servis i upiti (ocene 7–10)
Koristiti `ElasticsearchOperations` / `NativeQuery` (Spring Data ES 5) builder, NE samo izvedene metode iz repozitorijuma.

- [ ] **Po nazivu / opisu / PDF-opisu** (match na odgovarajuća polja) — ocena 6
- [ ] **Opseg broja review-a** — `range` query nad `reviewCount`, donja i/ili gornja granica opcione — ocena 7
- [ ] **Kombinacija (BooleanQuery)** — `bool` query; parametar koji bira **AND (must) ili OR (should)** između polja — ocena 8
- [ ] **PhraseQuery / PrefixQuery / FuzzyQuery iz forme** (ocena 9) — parsirati vrednost polja:
  - počinje/okružena `"..."` → `match_phrase`
  - sadrži `*` (npr. `Ivan M*`) → `prefix` / `wildcard`
  - počinje sa `~` (npr. `~rizika`) → `fuzzy` (empirijski `fuzziness`, npr. `AUTO` ili 2)
  - inače → običan `match`
- [ ] **MoreLikeThis** (ocena 9) — `more_like_this` nad poljima `naziv`, `opis`, `pdfOpis`; `min_term_freq`/`max_query_terms` empirijski.
- [ ] **Opseg prosečne ocene po kategorijama** (ocena 10) — `range` nad `avgNastup`/`avgZvukSvetlo`/`avgProstor`/`avgUkupno`, donja i/ili gornja granica opcione.
- [ ] **Sortiranje po nazivu** (ocena 10) — `sort` po `naziv.keyword`.
- [ ] **Highlighter** (ocena 10) — uključiti highlight nad `naziv`/`opis`/`pdfOpis`, vratiti `<em>` sažetke u rezultatu.

### 5.6 Kontroler (ocene 6–10)
- [ ] `Controller/LocationSearchController.java`:
  - `POST /api/search/locations` — telo sa svim parametrima pretrage (po poljima, range-ovi, AND/OR flag, sort) → vraća listu rezultata sa highlight sažecima
  - `GET /api/search/locations/{id}/more-like-this` — MoreLikeThis
  - `POST /api/locations/{id}/pdf` — upload PDF-a (parsira + indeksira `pdfOpis`)
  - `GET /api/locations/{id}/pdf` — download PDF-a
- [ ] `DTOs` za search request/response (uključiti highlight i `pdfDownloadUrl`).

### 5.7 Frontend (Angular) — UES pretraga
- [ ] Nova stranica/komponenta `search` (npr. pod `Location/`) sa formom:
  - polja: naziv, opis, opis-iz-PDF-a (sa podrškom za `"..."`, `*`, `~`)
  - range inputi: broj review-a (od/do), prosečna ocena po kategoriji (od/do)
  - izbor **AND/OR** operatora
  - izbor sortiranja (po nazivu)
- [ ] Prikaz rezultata: naziv, opis (iz UI-a, **ne** PDF), highlight sažetak (`[innerHTML]`), dugme **„Preuzmi PDF"**, dugme **„Slična mesta" (MoreLikeThis)**.
- [ ] Na formi/detalju mesta: **upload PDF-a** (za menadžera/admina).
- [ ] `services/` — `search-service.ts` koji gađa nove endpointe.

---

## 6. Finalni checklist (sve što treba da bude ✅)

**SVT/KVA ispravke**
- [ ] K5: skala ocena 1→10 (`@Max(10)`)
- [ ] K5: ocene opcione (skinuti `@NotNull`)
- [ ] K5: pojedinačne ocene u `ReviewResponseDTO` + mapper

**UES — ocena 6**
- [ ] docker-compose (ES + MinIO) + pom zavisnosti + config + `MinioConfig` bean/bucket-i
- [ ] MinIO storage servis; slike čitaju/pišu u MinIO
- [ ] Upload PDF-a uz mesto + parsiranje teksta (PDFBox/Tika)
- [ ] `LocationDocument` + indeks + **custom analyzer (ćir/lat, case-insensitive)**
- [ ] Indeksiranje/reindeks mesta
- [ ] Pretraga po nazivu / opisu / opisu-iz-PDF-a
- [ ] Prikaz rezultata (naziv + opis iz UI-a) + **download PDF-a**
- [ ] Angular search stranica (osnovna)

**UES — ocena 7**
- [ ] Pretraga po opsegu broja review-a (od/do, opcione granice)

**UES — ocena 8**
- [ ] BooleanQuery kombinacija polja sa AND/OR operatorom

**UES — ocena 9**
- [ ] PhraseQuery (`"..."`), PrefixQuery (`*`), FuzzyQuery (`~`) iz forme
- [ ] MoreLikeThis (naziv, opis, pdfOpis)

**UES — ocena 10**
- [ ] Pretraga po opsegu prosečne ocene po kategorijama
- [ ] Sortiranje rezultata po nazivu (`naziv.keyword`)
- [ ] Highlighter (dinamički sažetak u rezultatima)

---

## 7. Verifikacija (end-to-end)

1. `docker compose up -d` → proveriti ES na `http://localhost:9200` i MinIO konzolu na `http://localhost:9001`.
2. `cd backend && ./mvnw spring-boot:run` (Windows: `mvnw.cmd`). Proveriti da se indeks `locations` kreira sa custom analyzer-om: `GET http://localhost:9200/locations/_settings`.
3. Kreirati mesto + upload slike (proveriti da objekat postoji u MinIO bucket-u) + upload PDF-a; proveriti da `pdfOpis` u ES dokumentu sadrži tekst iz PDF-a (`GET /locations/_search`).
4. `cd frontend && npm install && npm start` → otvoriti search stranicu i testirati redom:
   - naziv/opis/PDF-opis pretragu; ćirilica vs latinica isti rezultat; veliko/malo slovo svejedno (ocena 6)
   - opseg broja review-a (ocena 7)
   - AND vs OR kombinaciju (ocena 8)
   - `"fraza"`, `prefiks*`, `~fazi` (ocena 9) + „Slična mesta" (MoreLikeThis)
   - opseg ocene po kategoriji + sortiranje po nazivu + highlight sažetak (ocena 10)
   - download PDF-a iz rezultata (ocena 6)
5. Regresija SVT/KVA: ostaviti recenziju sa ocenom 8 (mora proći nakon ispravke skale), proveriti da pojedinačne ocene stižu na frontend.

---

### Pomoćni fajlovi (scratch, mogu se obrisati)
Tokom analize napravljeni su `_spec_extracted.txt`, `_spec_colors.txt`, `_spec_grades.txt` (izvučen tekst/boje iz PDF-a). Nisu deo projekta — slobodno obrisati.
