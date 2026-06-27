# Projektni zadatak: SVT i KVT

Student: SR XX/YYYY

---

## Pokretanje — JEDNA KOMANDA (sve preko Dockera)

Iz root foldera projekta (uz pokrenut Docker Desktop):

```bash
docker compose up --build
```

Ovo diže ceo stack (prvo pokretanje traje par minuta — builduje backend i frontend
i povlači ES/MySQL/MinIO image-e). Backend čeka da MySQL, Elasticsearch i MinIO budu
spremni (healthcheck) pa tek onda kreće, tako da se ES indeks i MinIO bucket-i naprave iz prve.

Kad sve digne:
- **Aplikacija (frontend):** http://localhost:4200
- **Backend API:** http://localhost:8080
- **Elasticsearch:** http://localhost:9200
- **Kibana (debug):** http://localhost:5601
- **MinIO konzola:** http://localhost:9001  (`minioadmin` / `minioadmin`)
- **MySQL:** localhost:3306 (baza `svtkvt`)

Stranica pretrage: link „Pretraga" u navbaru (ruta `/search`).

Zaustavljanje: `docker compose down` (dodaj `-v` da obrišeš i podatke/volume-ove).

Pri startu backend automatski kreira ES indeks `locations` sa custom analyzer-om
`sr_custom` (ćirilica↔latinica + lowercase + asciifolding) i seed-uje sva mesta iz baze.
Provera: `GET http://localhost:9200/locations/_settings`.

> Napomena: ako se neki servis dugo prikazuje kao „unhealthy", pogledaj logove tog
> servisa (`docker compose logs <servis>`). ES na Windows-u (WSL2) ponekad traži veći
> `vm.max_map_count` — Docker Desktop to obično podešava sam.

### Alternativa: lokalni dev (bez dockerizovanog app-a)

Ako ti treba hot-reload tokom razvoja, možeš dići samo infrastrukturu pa app lokalno:

```bash
docker compose up -d mysql elasticsearch minio kibana
cd backend && ./mvnw spring-boot:run     # Windows: mvnw.cmd spring-boot:run
cd frontend && npm install && npm start   # http://localhost:4200
```

### UES endpointi
- `POST /api/search/locations` — glavna pretraga (telo = parametri pretrage)
- `GET  /api/search/locations/{id}/more-like-this` — slična mesta (MoreLikeThis)
- `POST /api/search/reindex` — ručni reindeks (admin)
- `POST /api/locations/{id}/pdf` — upload PDF opisa (admin/menadžer)
- `GET  /api/locations/{id}/pdf` — download PDF-a

### Sintaksa pretrage (polja naziv/opis/PDF opis)
- `"tačna fraza"` → PhraseQuery
- `prefiks*` → PrefixQuery (match_phrase_prefix)
- `~približno` → FuzzyQuery
- inače → obična `match` pretraga
