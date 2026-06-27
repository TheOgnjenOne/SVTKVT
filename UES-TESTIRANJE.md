# UES — Kako da testiraš sve (korak po korak)

> Praktičan vodič: prvo da proradi vizuelno, pa provera slika/MinIO, pa **poenta PDF pretrage**,
> pa test svake ocene 6→10. Komande su za PowerShell/CMD na Windows-u.

---

## 0. ⚠️ PRVO OVO — inače "ništa se ne menja"

Frontend se **build-uje u nginx image** (nije `ng serve`). Sve izmene koje sam uradio
(novi izgled pretrage + popravka slika) su **nevidljive dok ne rebuild-uješ frontend**:

```bash
docker compose up -d --build frontend
```

Backend izmena (uklonjen pogrešan "default image" fallback) zahteva rebuild backenda:

```bash
docker compose up -d --build backend
```

Ili sve odjednom: `docker compose up -d --build`.

Posle toga otvori **http://localhost:4200** i uloguj se. Stranica **Pretraga** je u navbar-u.

---

## 1. Provera da infrastruktura radi

```bash
docker ps                          # svih 6 kontejnera "Up"
```

- **Elasticsearch:** otvori http://localhost:9200 → vrati JSON (verzija itd.)
- **MinIO konzola:** http://localhost:9001 → login `minioadmin` / `minioadmin`
  - U levom meniju **Buckets** → vidiš `images` i `pdfs`. Klikni → **Object Browser** vidi fajlove.
- **Kibana (debug ES):** http://localhost:5601

Brza provera indeksa iz terminala:

```bash
curl http://localhost:9200/locations/_count
curl "http://localhost:9200/locations/_search?size=20&_source=naziv,imageId,hasPdf"
```

---

## 2. Zašto se slike NISU prikazivale (i šta je popravljeno)

**MinIO radi** — slika je bila uredno otpremljena u bucket `images`. Problem su bile **dve stvari**, obe popravljene:

1. **Mesto nije bilo povezano sa slikom.** U bazi je `locations.image_id` bio `NULL` iako je
   slika postojala u `images` tabeli i u MinIO. Razlog: stari kod je pri kreiranju mesta **bez**
   slike lepio nepostojeću "default" sliku (`getImageById(1L)`) → ostane `null`.
   → Sad: bez slike ostaje prazno i frontend prikaže `/defaultLoc.png`. Tvoje postojeće mesto
   `Test1` sam povezao sa slikom koju si već uploadovao.

2. **Pogrešan URL za sliku na stranici „Mesta".** Kod je gradio `…/uploads/<path>` (stari
   fajl-sistem), a slike se sad serviraju iz MinIO preko `…/api/images/<id>`.
   → Popravljeno u `location-page.ts`.

**Kako da testiraš slike posle rebuild-a:**

- Stranica **Mesta**: `Test1` sad treba da ima sliku (ne placeholder).
- Stranica **Pretraga**: kartica za `Test1` treba da prikaže istu sliku levo.
- **Novo mesto sa slikom** (kao admin): *Mesta → Dodaj mesto*, izaberi sliku → sačuvaj.
  Slika treba odmah da se vidi na obe stranice.
- Direktna provera da backend servira bajtove (treba `200 image/png`):
  ```bash
  curl -s -o NUL -w "%{http_code} %{content_type}\n" http://localhost:8080/api/images/1
  ```

> Napomena: ako slici dodaš sliku kroz *izmenu mesta*, ona se **automatski reindeksira**, pa se
> odmah pojavi i na stranici Pretraga (jer ES dobije `imageId`).

---

## 3. 🎯 POENTA: pretraga PO TEKSTU IZ PDF-a

Ovo je suština UES dela. Kad menadžer/admin **zakači PDF uz mesto**, backend ga **parsira**
(izvuče tekst) i **indeksira** kao polje `pdfOpis`. Onda mesto možeš da nađeš tako što ukucaš
**reč koja se nalazi UNUTAR tog PDF-a** — iako te reči nigde nema u nazivu ni opisu mesta.

Tvoj PDF (UES specifikacija) je već zakačen za `Test1` i indeksiran. U njemu se nalaze npr.
reči: **VAT**, **kVA**, **Consumption**, **Validation**, **Back Billing**, **Capacity Charge**.

**Test (stranica Pretraga):**

1. U sidebar polje **„Opis iz PDF-a"** ukucaj `VAT` → klikni *Primeni filtere*.
   → `Test1` se pojavi, a u kartici vidiš **highlight** (žuto obeležen deo teksta iz PDF-a).
2. Probaj i `kVA`, pa `Consumption`.
3. Tačna fraza: ukucaj `"Consumption Validation"` (sa navodnicima) → vraća samo ako se ta
   tačna fraza pojavljuje.
4. Klikni **„Preuzmi PDF"** na kartici → skine se isti PDF nazad.

> Ako hoćeš da zakačiš **novi** PDF: u sidebar-u (samo admin/menadžer) je panel
> **„Postavi PDF opis mesta"** → izaberi mesto → izaberi `.pdf` iz Downloads → *Postavi i indeksiraj*.
> Zatim ga odmah nađi po nekoj reči iz tog PDF-a.

---

## 4. Test po ocenama (6 → 10)

Sve se testira na stranici **Pretraga**. Pre testa dobro je imati 2–3 mesta sa različitim
opisima, ocenama i bar jednim PDF-om (možeš da reindeksiraš sve kao admin — vidi §5).

### Ocena 6 — osnovna pretraga + analyzer + PDF + download
- **Naziv:** u glavnu traku gore ukucaj deo naziva (npr. `Test`) → *Pretraži*.
- **Opis:** sidebar „Opis (iz UI-a)" → reč iz opisa mesta.
- **PDF opis:** vidi §3.
- **Ćirilica ↔ latinica (custom analyzer):** ako mesto ima naziv/opis na ćirilici
  (npr. „Београд"), pretraga `Beograd` (latinica) treba da ga nađe — i obrnuto.
- **Veliko/malo slovo:** `BEOGRAD`, `beograd`, `BeOgRaD` → isti rezultat.
- **Download PDF-a:** dugme „Preuzmi PDF" na kartici.

### Ocena 7 — opseg broja utisaka
- Sidebar **„Broj utisaka"** → unesi samo `od` (npr. 1), ili samo `do`, ili oba.
- Vraća mesta čiji `reviewCount` upada u opseg.

### Ocena 8 — kombinacija polja (AND / ILI)
- Popuni **dva** polja (npr. Naziv = `Test`, Opis iz PDF-a = `nepostojeca_rec`).
- **I (AND):** nema rezultata (oba moraju da se poklope).
- Prebaci na **ILI (OR)** → `Test1` se vrati (dovoljno da se jedno poklopi).

### Ocena 9 — phrase / prefiks / fuzzy + slična mesta
U bilo kom tekstualnom polju (naziv/opis/PDF):
- **Fraza:** `"Consumption Validation"` (navodnici) → tačna fraza.
- **Prefiks:** `Cons*` → hvata `Consumption`, `Consecutive`…
- **Fuzzy (greška u kucanju):** `~Consuption` (fali slovo) → svejedno nađe.
- **Slična mesta (MoreLikeThis):** dugme „Slična mesta" na kartici → vraća mesta slična po
  nazivu/opisu/PDF-tekstu.

### Ocena 10 — ocene po kategorijama + sort + highlight
- **Opseg ocene:** sidebar „Prosečna ocena → Nastup/Zvuk i svetlo/Prostor/Ukupan utisak", od–do.
  (Da bi imalo smisla, mesto mora da ima utiske sa tim ocenama.)
- **Sortiranje po nazivu:** gore desno „Sortiraj → Po nazivu", pa A–Š / Š–A.
- **Highlight:** kad pretražuješ po reči, u kartici je žuto obeležen deo gde se reč pojavljuje.

> ⚠️ Da bi ocene po kategorijama (ocena 10) imale vrednosti, ostavi par utisaka sa ocenama 1–10
> na nekom mestu, pa **reindeksiraj** (§5) da se prosečne ocene upišu u ES.

---

## 5. Reindeks (osvežavanje ES indeksa) — kao admin

Mesta se automatski indeksiraju pri kreiranju/izmeni i pri dodavanju PDF-a. Ali ako menjaš
podatke direktno u bazi ili dodaješ utiske, pokreni ručni reindeks (potreban admin JWT token):

```bash
# 1) uloguj se i uzmi token
curl -s -X POST http://localhost:8080/api/auth/login ^
  -H "Content-Type: application/json" ^
  -d "{\"email\":\"<admin_email>\",\"password\":\"<lozinka>\"}"

# 2) pokreni reindeks (zameni <TOKEN>)
curl -s -X POST http://localhost:8080/api/search/reindex -H "Authorization: Bearer <TOKEN>"
```

Provera posle reindeksa:

```bash
curl "http://localhost:9200/locations/_search?_source=naziv,reviewCount,avgUkupno,imageId,hasPdf"
```

---

## 6. Kratka regresija SVT/KVA (da se ništa nije pokvarilo)
- Ostavi utisak sa ocenom **8** (skala je 1–10) → mora da prođe.
- Otvori mesto → pojedinačne ocene (nastup/zvuk-svetlo/prostor) stižu na frontend.
- Slike mesta se vide (§2).

---

### TL;DR
1. `docker compose up -d --build` → otvori http://localhost:4200/search
2. Slike: rebuild + `Test1` je već povezan; novo mesto sa slikom radi.
3. PDF pretraga = ukucaj reč **iz PDF-a** (`VAT`, `kVA`, `"Consumption Validation"`) u „Opis iz PDF-a".
4. Ocene 7–10: opsezi, AND/OR, `"fraza"`/`prefiks*`/`~fuzzy`, sort po nazivu, highlight.
