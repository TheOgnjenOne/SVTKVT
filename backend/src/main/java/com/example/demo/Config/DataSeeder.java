package com.example.demo.Config;

import com.example.demo.Enums.UserRole;
import com.example.demo.Model.*;
import com.example.demo.Repository.*;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Puni MySQL lažnim podacima za testiranje UES funkcionalnosti.
 * Idempotentno — ne radi ništa ako lokacije već postoje.
 * ES indeksiranje preuzima ElasticsearchIndexInitializer koji se pokreće posle.
 */
@Component
@Order(1)
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final ILocationRepository locationRepo;
    private final IUserRepository userRepo;
    private final IEventRepository eventRepo;
    private final IReviewRepository reviewRepo;
    private final ILocationManagerRepository managerRepo;
    private final PasswordEncoder passwordEncoder;
    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    public DataSeeder(ILocationRepository locationRepo, IUserRepository userRepo,
                      IEventRepository eventRepo, IReviewRepository reviewRepo,
                      ILocationManagerRepository managerRepo, PasswordEncoder passwordEncoder,
                      MinioClient minioClient, MinioProperties minioProperties) {
        this.locationRepo = locationRepo;
        this.userRepo = userRepo;
        this.eventRepo = eventRepo;
        this.reviewRepo = reviewRepo;
        this.managerRepo = managerRepo;
        this.passwordEncoder = passwordEncoder;
        this.minioClient = minioClient;
        this.minioProperties = minioProperties;
    }

    @Override
    public void run(String... args) {
        if (locationRepo.count() == 0) {
            log.info("DataSeeder: upisujem podatke...");
            List<User> users = seedUsers();
            List<Location> locs = seedLocations();
            seedManagers(locs, users);
            List<Event> events = seedEvents(locs);
            seedReviews(events, users);
            updateTotalRatings(locs);
            log.info("DataSeeder: završeno — {} lokacija, {} evenata, {} korisnika.", locs.size(), events.size(), users.size());
        } else {
            log.info("DataSeeder: podaci već postoje, preskačem seed.");
        }
        seedPdfsIfNeeded();
    }

    // ─── Korisnici ────────────────────────────────────────────────────────────

    private List<User> seedUsers() {
        String p = passwordEncoder.encode("test123");
        List<User> saved = userRepo.saveAll(List.of(
            user("Admin",                "admin@test.com",          p, UserRole.ADMIN,   "0600000000", LocalDate.of(1990,  1,  1), "Adminova 1",                   "Beograd"),
            user("Marko Marković",       "marko@test.com",          p, UserRole.USER,    "0641234567", LocalDate.of(1995,  3, 15), "Knez Mihailova 12",            "Beograd"),
            user("Ana Nikolić",          "ana@test.com",            p, UserRole.USER,    "0651111222", LocalDate.of(1998,  7, 22), "Bulevar Kralja Aleksandra 50", "Beograd"),
            user("Stefan Jovanović",     "stefan@test.com",         p, UserRole.USER,    "0601234321", LocalDate.of(1992, 11,  5), "Zmaj Jovina 3",                "Novi Sad"),
            user("Jelena Petrović",      "jelena@test.com",         p, UserRole.USER,    "0621234000", LocalDate.of(2000,  1, 30), "Cara Dušana 8",                "Beograd"),
            user("Nikola Stojanović",    "nikola@test.com",         p, UserRole.USER,    "0631000222", LocalDate.of(1997,  6, 18), "Svetogorska 20",               "Beograd"),
            user("Milica Pavlović",      "milica@test.com",         p, UserRole.USER,    "0641999000", LocalDate.of(2001,  4, 12), "Terazije 7",                   "Beograd"),
            user("Luka Đorđević",        "luka@test.com",           p, UserRole.USER,    "0652221100", LocalDate.of(1994,  8, 25), "Dunavska 15",                  "Novi Sad"),
            user("Dragana Vasić",        "dragana.manager@test.com",p, UserRole.MANAGER, "0641112233", LocalDate.of(1985,  4, 10), "Nemanjina 5",                  "Beograd"),
            user("Zoran Đorđević",       "zoran.manager@test.com",  p, UserRole.MANAGER, "0652223344", LocalDate.of(1980,  9, 25), "Jovanova 15",                  "Novi Sad"),
            user("Ivana Milošević",      "ivana.manager@test.com",  p, UserRole.MANAGER, "0663334455", LocalDate.of(1988,  2, 14), "Kralja Milana 3",              "Beograd")
        ));
        log.info("DataSeeder: {} korisnika.", saved.size());
        return saved;
    }

    private User user(String name, String email, String pass, UserRole role,
                      String phone, LocalDate birthday, String address, String city) {
        User u = new User();
        u.setName(name); u.setEmail(email); u.setPassword(pass); u.setRole(role);
        u.setPhoneNumber(phone); u.setBirthday(birthday); u.setAddress(address); u.setCity(city);
        return u;
    }

    // ─── Lokacije ─────────────────────────────────────────────────────────────

    private List<Location> seedLocations() {
        List<Location> saved = locationRepo.saveAll(List.of(

            location("Klub Fabrika",
                "Cetinjska 15, Beograd", "Klub",
                "Fabrika je jedan od najpopularnijih noćnih klubova u Beogradu, smešten u srcu Savamale. " +
                "Prostran industrijski prostor sa sjajnom akustikom i modernom zvučnom i svetlosnom opremom. " +
                "Redovno organizuje nastupe domaćih i stranih DJ-eva, tehno i elektronske muzike. " +
                "Kapacitet kluba je oko 1500 posetilaca, sa nekoliko barova i VIP zonom."),

            location("Sava Centar",
                "Milentija Popovića 9, Beograd", "Koncertna dvorana",
                "Sava Centar je višenamenska kongresna i kulturna ustanova sa kapacitetom od 4000 mesta. " +
                "Idealan prostor za velike pop i rock koncerte, konferencije i spektakle. " +
                "Opremljena najsavremenijim audio-vizuelnim sistemima i profesionalnom rasvjetom. " +
                "Više od 40 godina tradicije organizacije kulturnih i muzičkih događaja."),

            location("Dom Omladine Beograd",
                "Makedonska 22/24, Beograd", "Kulturni centar",
                "Dom Omladine je kulturni centar u centru Beograda koji neguje alternativnu i indie kulturu. " +
                "Organizuje konceptualne izložbe, književne večeri, filmske projekcije i muzičke nastupe. " +
                "Poznat po sceni za mlade umetnike, eksperimentalnu muziku i indie bendove. " +
                "Omiljeno mesto beogradske alternative i boema."),

            location("Hangar 18",
                "Bulevar Cara Lazara 100, Novi Sad", "Klub",
                "Hangar 18 je industrijski noćni klub u Novom Sadu poznat po tehno i elektronskoj muzičkoj sceni. " +
                "Bivši industrijski prostor pretvoren u spektakularni noćni klub sa tri sale i dve bine. " +
                "Domaćin EXIT after-party događaja, techno žurki i regionalnih elektronskih festivala. " +
                "Kapacitet kluba prelazi 2000 posetilaca, sa state-of-the-art zvučnim sistemom."),

            location("Štark Arena",
                "Bulevar Arsenija Čarnojevića 58, Beograd", "Koncertna dvorana",
                "Štark Arena je najveća zatvorena sportsko-muzička arena u jugoistočnoj Evropi. " +
                "Kapacitet do 22.000 posetilaca za koncerte i sportska takmičenja. " +
                "Domaćin je svetskih zvezda poput Madonne, Metallice, Depeche Mode i mnogih drugih. " +
                "Opremljena vrhunskim pozornicama, LED ekranima i profesionalnim zvučnim sistemima."),

            location("Jazzbina",
                "Skadarlija 42, Beograd", "Jazz bar",
                "Jazzbina je intimni jazz bar u srcu Skadarlije, beogradske bohemske četvrti. " +
                "Svake večeri nastupaju živi jazz i blues bendovi iz Srbije i regiona. " +
                "Prijatna atmosfera, odlična karta pića, vina i domaće hrane. " +
                "Idealno mesto za ljubitelje jazz i blues muzike koji traže autentičan doživljaj."),

            location("Kolarčeva Zadužbina",
                "Studentski trg 5, Beograd", "Koncertna dvorana",
                "Kolarčeva zadužbina je istorijska koncertna dvorana u centru Beograda. " +
                "Izvanredna akustika i klasičan ambijent savršeni su za klasičnu muziku, operu i džez nastupe. " +
                "Sadrži Veliku salu sa 1000 mesta i Malu salu sa 500 mesta. " +
                "Domaćin Beogradske filharmonije i međunarodnih solista već decenijama."),

            location("Šećerana",
                "Karaburma bb, Beograd", "Festival prostor",
                "Šećerana je napuštena fabrika šećera pretvorena u multimedijalnu festival zonu. " +
                "Jedna od najvećih open-air lokacija u Srbiji sa kapacitetom preko 30.000 posetilaca. " +
                "Domaćin Beer Fest-a, Exit spin-off događaja i brojnih muzičkih festivala. " +
                "Autentična industrijska atmosfera čini ovo mesto jedinstvenim na regionalnoj muzičkoj sceni."),

            location("Mixer House",
                "Krunska 58, Beograd", "Klub",
                "Mixer House je poznati beogradski noćni klub i bar u prestižnoj lokaciji na Vračaru. " +
                "Specijalizovan za house i techno muziku, sa redovnim nastupima domaćih i stranih DJ-eva. " +
                "Moderan interior, odlična zvučna i svetlosna oprema, kapacitet oko 800 posetilaca. " +
                "Popularna destinacija za ljubitelje elektronske muzike od 2010. godine."),

            location("Studentski kulturni centar SKC",
                "Kralja Milana 48, Beograd", "Kulturni centar",
                "SKC je legendarni studentski kulturni centar u Beogradu koji postoji od 1969. godine. " +
                "Organizuje koncerte alternativne, indie i eksperimentalne muzike, filmske projekcije i izložbe. " +
                "Istorijsko mesto beogradskog punk, new wave i rock pokreta. " +
                "I danas aktivno mesto kulturnog života sa više sala i klubskom scenom."),

            location("Klub Pulse",
                "Bulevar Mihajla Pupina 10, Novi Sad", "Klub",
                "Klub Pulse je moderna venue u Novom Sadu sa naglaskom na house, techno i drum&bass muziku. " +
                "Prostrana sala sa vrhunskim zvučnim sistemom i atraktivnom vizuelnom produkcijom. " +
                "Redovni gosti su poznati regionalni i evropski DJ-evi sa prepoznatljivih elektronskih labela. " +
                "Kapacitet oko 1200 posetilaca, sa rooftop terasom otvorenom leti."),

            location("Bitef Art Café",
                "Skender-begova 2, Beograd", "Bar",
                "Bitef Art Café je kultni beogradski bar uz Bitef pozorište, centar alternativne scene. " +
                "Organizuje akustične nastupe, spoken word večeri, književne razgovore i art izložbe. " +
                "Prijatna kućna atmosfera, domaći kokteli i odlična muzička selekcija. " +
                "Omiljeno okupljaliste glumaca, pisaca, muzičara i umetnika već 30 godina."),

            location("Gradski Park Open Air",
                "Štrosmajerov trg, Novi Sad", "Festival prostor",
                "Gradski park u centru Novog Sada tokom leta postaje live muzička scena pod vedrim nebom. " +
                "Organizuju se jazz, klasična i folk muzika, besplatni koncerti i food festivali. " +
                "Idilična atmosfera u zelenilu, idealna za porodice, parove i sve ljubitelje muzike. " +
                "Kapacitet do 5000 posetilaca, prirodni ambijent i odlična akustika."),

            location("Klub 20/44",
                "Kej žrtava racije 1, Novi Sad", "Klub",
                "Klub 20/44 je popularna noćna venue na novosadskom keju, uz Dunav. " +
                "Organizuje tehno, house i elektronsku muziku sa naglaskom na lokalne DJ-eve i producente. " +
                "Kombinacija unutrašnjeg prostora i letnje bašte sa pogledom na Dunav čini ga posebnim. " +
                "Kapacitet 600 posetilaca, prepoznatljiv po autentičnoj underground atmosferi."),

            location("Nišville Jazz Club",
                "Trg Kralja Milana 3, Niš", "Jazz bar",
                "Nišville Jazz Club je srce niške jazz scene i odraz tradicije čuvenog Nišville festivala. " +
                "Svake nedelje i petka nastupaju jazz, blues i soul bendovi iz celog regiona. " +
                "Topla atmosfera, odabrana vina, craft piva i domaći specijaliteti. " +
                "Popularno mesto za ljubitelje džeza koji traže autentičan domaći jazz doživljaj."),

            location("Čekaonica",
                "Savska 5, Beograd", "Bar",
                "Čekaonica je kultni beogradski indie bar sa živom muzičkom scenom. " +
                "Organizuje akustične nastupe indie, folk i alternativnih bendova svake srede i petka. " +
                "Opuštena atmosfera, selekcija domaćeg piva i odabrana muzika iz vinyl kolekcije. " +
                "Omiljeno mesto za ljubitelje indie muzike, studente i kreativne profesionalce."),

            location("Palata Srbija Konferencijski centar",
                "Bulevar Mihajla Pupina 2, Beograd", "Konferencijski centar",
                "Palata Srbija je prestižni konferencijski i kongresni centar u Beogradu. " +
                "Deset modernih sala kapaciteta od 50 do 2000 mesta, idealan za konferencije, seminare i gala večeri. " +
                "Vrhunska tehnička opremljenost, profesionalni organizacioni tim i restoran svetske klase. " +
                "Domaćin državnih i međunarodnih skupova, korporativnih proslava i muzičkih gala programa."),

            location("Magacin u Kraljevića Marka",
                "Kraljevića Marka 4, Beograd", "Kulturni centar",
                "Magacin u Kraljevića Marka je nezavisni kulturni centar smešten u prelepom istorijskom magacinu. " +
                "Organizuje savremene plesne predstave, eksperimentalnu muziku i interdisciplinarne umetničke projekte. " +
                "Jedan od najvažnijih prostora nezavisne kulturne scene u Srbiji. " +
                "Betonski podovi, visoki plafoni i industrijska atmosfera pružaju jedinstven estetski doživljaj."),

            location("Exit Main Stage – Petrovaradin",
                "Petrovaradinska tvrđava, Novi Sad", "Festival prostor",
                "Petrovaradinska tvrđava domaćin je čuvenog Exit festivala, jednog od top 5 festivala u Evropi. " +
                "Više od 30 bina, kapacitet 30.000 posetilaca noćno i 50.000 dnevno, 4 dana neprekidne muzike. " +
                "Na Main Stage su nastupali Arcade Fire, The Killers, Grace Jones, David Guetta, Carl Cox. " +
                "Jedinstvena atmosfera istorijske tvrđave pretvara Exit u neponovljivo muzičko iskustvo."),

            location("Klub Mladih Kragujevac",
                "Trg Slobode 2, Kragujevac", "Klub",
                "Klub Mladih je centar noćnog i kulturnog života Kragujevca već 25 godina. " +
                "Organizuje techno, house i balkanske ritmove sa lokalnim DJ-evima i povremenim gostima. " +
                "Jedina prava klub venue u gradu sa profesionalnom zvučnom i svetlosnom opremom. " +
                "Kapacitet 700 posetilaca, mlada publika i živahna atmosfera svake petke i subote.")
        ));
        log.info("DataSeeder: {} lokacija.", saved.size());
        return saved;
    }

    private Location location(String name, String address, String type, String description) {
        Location l = new Location();
        l.setName(name); l.setAddress(address); l.setType(type); l.setDescription(description);
        return l;
    }

    // ─── Menadžeri ────────────────────────────────────────────────────────────

    private void seedManagers(List<Location> locs, List<User> users) {
        User m1 = users.get(7);  // Dragana
        User m2 = users.get(8);  // Zoran
        User m3 = users.get(9);  // Ivana
        LocalDate today = LocalDate.now();

        for (int i = 0; i < 7; i++)
            managerRepo.save(buildManager(locs.get(i), m1, today.minusMonths(8)));
        for (int i = 7; i < 13; i++)
            managerRepo.save(buildManager(locs.get(i), m2, today.minusMonths(4)));
        for (int i = 13; i < locs.size(); i++)
            managerRepo.save(buildManager(locs.get(i), m3, today.minusMonths(2)));
    }

    private LocationManager buildManager(Location loc, User mgr, LocalDate start) {
        LocationManagerId id = new LocationManagerId(loc.getId(), mgr.getId());
        LocationManager lm = new LocationManager();
        lm.setId(id); lm.setLocation(loc); lm.setUser(mgr); lm.setStartDate(start);
        return lm;
    }

    // ─── Događaji ─────────────────────────────────────────────────────────────

    private List<Event> seedEvents(List<Location> L) {
        LocalDateTime now    = LocalDateTime.now();
        LocalDateTime today  = now.withHour(21).withMinute(0).withSecond(0);
        LocalDateTime tom    = today.plusDays(1);
        LocalDateTime past1  = now.minusDays(7);
        LocalDateTime past2  = now.minusDays(14);
        LocalDateTime past3  = now.minusDays(30);
        LocalDateTime past4  = now.minusDays(45);
        LocalDateTime past5  = now.minusDays(60);
        LocalDateTime fut1   = now.plusDays(7);
        LocalDateTime fut2   = now.plusDays(14);
        LocalDateTime fut3   = now.plusDays(30);

        List<Event> saved = eventRepo.saveAll(List.of(

            // Klub Fabrika (0) — techno/elektronika
            event("Techno Night Vol.3",          L.get(0),  "Tehno",             past3,  "2500.00", false),
            event("Exit After Party 2025",       L.get(0),  "Festival",          past2,  "1500.00", false),
            event("Drum & Bass Marathon",        L.get(0),  "DJ Nastup",         past1,  "2000.00", false),
            event("Fabrika Noć Elektronike",     L.get(0),  "DJ Nastup",         today,  "2200.00", false),
            event("Industrial Rave",             L.get(0),  "Tehno",             fut1,   "2000.00", false),

            // Sava Centar (1) — pop/rock
            event("Ceca – Povratak na scenu",    L.get(1),  "Pop koncert",       past4,  "3500.00", false),
            event("Balkan Music Awards 2025",    L.get(1),  "Dodela nagrada",    past3,  "0.00",    false),
            event("Riblja Čorba 45 godina",      L.get(1),  "Rock koncert",      past1,  "4000.00", false),
            event("Noc Muzike Sava",             L.get(1),  "Pop koncert",       today,  "2800.00", false),
            event("New Year Countdown Sava",     L.get(1),  "Spektakl",          fut3,   "5000.00", false),

            // Dom Omladine (2) — alternativa/indie
            event("Alternativni Film Festival",  L.get(2),  "Film",              past3,  "800.00",  false),
            event("Spoken Word Evening",         L.get(2),  "Književna večer",   past2,  "500.00",  false),
            event("Indie Belgrade Vol.2",        L.get(2),  "Indie rock",        past1,  "1200.00", false),
            event("Poetry & Music Night",        L.get(2),  "Književna večer",   today,  "600.00",  false),
            event("DOM Indie Showcase",          L.get(2),  "Indie rock",        fut2,   "1000.00", false),

            // Hangar 18 (3) — tehno/elektronika
            event("Industrial Techno Night",     L.get(3),  "Tehno",             past3,  "1200.00", false),
            event("Hangar Massive Attack",       L.get(3),  "Elektronska muzika",past2,  "1800.00", false),
            event("4AM Techno Session",          L.get(3),  "Tehno",             past1,  "1500.00", false),
            event("Hangar Elektronika Fest",     L.get(3),  "DJ Nastup",         today,  "1600.00", false),
            event("Hardstyle Invasion",          L.get(3),  "Elektronska muzika",fut1,   "1500.00", false),

            // Štark Arena (4) — mega koncerti
            event("Metallica World Tour",        L.get(4),  "Rock koncert",      past5,  "7500.00", false),
            event("David Guetta DJ Mega Set",    L.get(4),  "DJ Nastup",         past3,  "5000.00", false),
            event("Rammstein Serbia",            L.get(4),  "Rock koncert",      past1,  "8000.00", false),
            event("Amadeus Mozart Symphony",     L.get(4),  "Klasična muzika",   fut2,   "3500.00", false),

            // Jazzbina (5) — jazz/blues
            event("Jazz Večeras – Sreda",        L.get(5),  "Jazz",              past2,  "1000.00", true),
            event("Blues Night Special",         L.get(5),  "Blues",             past1,  "800.00",  false),
            event("Jazzbina Petak – Live",       L.get(5),  "Jazz",              today,  "1000.00", true),
            event("Swing & Soul Evening",        L.get(5),  "Jazz",              tom,    "900.00",  false),

            // Kolarac (6) — klasična muzika
            event("Filharmonija – Betoven",      L.get(6),  "Klasična muzika",   past3,  "2000.00", false),
            event("Jazz & Wine Festival",        L.get(6),  "Jazz",              past2,  "1500.00", false),
            event("Kolarac Kamerni Kvartet",     L.get(6),  "Klasična muzika",   past1,  "1800.00", false),
            event("Gala Koncert – Pucini",       L.get(6),  "Opera",             fut1,   "2500.00", false),

            // Šećerana (7) — festivali
            event("Beer Fest 2025",              L.get(7),  "Festival",          past4,  "0.00",    false),
            event("Elektropionir Outdoor",       L.get(7),  "Elektronska muzika",past3,  "1800.00", false),
            event("Šećerana Summer Rave",        L.get(7),  "Tehno",             past1,  "2000.00", false),
            event("Šećerana Oktoberfest",        L.get(7),  "Festival",          fut3,   "500.00",  false),

            // Mixer House (8) — house/tehno
            event("House Party Vračar",          L.get(8),  "House muzika",      past2,  "1500.00", false),
            event("Mixer Late Night",            L.get(8),  "DJ Nastup",         past1,  "1200.00", false),
            event("Mixer Subota Noć",            L.get(8),  "House muzika",      today,  "1400.00", false),
            event("Techno Invasion Mixer",       L.get(8),  "Tehno",             fut1,   "1300.00", false),

            // SKC (9) — alternativa/indie
            event("SKC Punk Revival",            L.get(9),  "Punk rock",         past3,  "800.00",  false),
            event("Indie Session SKC",           L.get(9),  "Indie rock",        past2,  "700.00",  false),
            event("New Wave Night",              L.get(9),  "New wave",          past1,  "900.00",  false),
            event("SKC Eksperimentalna Muzika",  L.get(9),  "Eksperimentalna",   today,  "600.00",  false),

            // Klub Pulse (10) — tehno/house Novi Sad
            event("Pulse Opening Night",         L.get(10), "DJ Nastup",         past3,  "1500.00", false),
            event("Pulse Techno Saturday",       L.get(10), "Tehno",             past1,  "1400.00", false),
            event("Pulse vs Hangar",             L.get(10), "Elektronska muzika",today,  "1600.00", false),
            event("Pulse Rooftop Summer",        L.get(10), "House muzika",      fut2,   "1800.00", false),

            // Bitef Art Café (11) — spoken word/akustika
            event("Akustična Večer u Bitefu",    L.get(11), "Akustična muzika",  past2,  "600.00",  false),
            event("Bitef Spoken Word",           L.get(11), "Književna večer",   past1,  "500.00",  false),
            event("Folk & Wine Bitef",           L.get(11), "Folk muzika",       today,  "700.00",  false),

            // Gradski Park (12) — besplatni open air
            event("Jazz u Parku – Lepo je biti", L.get(12), "Jazz",              past3,  "0.00",    false),
            event("Folk Festival Gradski Park",  L.get(12), "Folk muzika",       past1,  "0.00",    false),
            event("Letnji Koncert – Klasika",    L.get(12), "Klasična muzika",   today,  "0.00",    false),
            event("Food & Music Festival NS",    L.get(12), "Festival",          fut2,   "0.00",    false),

            // Klub 20/44 (13) — underground Novi Sad
            event("Kej Party 20/44",             L.get(13), "House muzika",      past2,  "1000.00", false),
            event("Underground Techno 20/44",    L.get(13), "Tehno",             past1,  "1200.00", false),
            event("20/44 Dunav Noć",             L.get(13), "DJ Nastup",         today,  "1100.00", false),

            // Nišville Jazz Club (14) — jazz Niš
            event("Nišville Petak Jazz",         L.get(14), "Jazz",              past2,  "800.00",  true),
            event("Blues & Soul Niš",            L.get(14), "Blues",             past1,  "700.00",  false),
            event("Nišville Jazz Session",       L.get(14), "Jazz",              today,  "800.00",  true),

            // Čekaonica (15) — indie/alternativa
            event("Indie Night Čekaonica",       L.get(15), "Indie rock",        past2,  "700.00",  false),
            event("Folk Akustika Srijeda",       L.get(15), "Folk muzika",       past1,  "600.00",  false),
            event("Vinyl Night Čekaonica",       L.get(15), "DJ Nastup",         today,  "500.00",  false),

            // Palata Srbija (16) — konferencije/gala
            event("IT Konferencija Srbija 2025", L.get(16), "Konferencija",      past3,  "0.00",    false),
            event("Gala Večer Ministartva",      L.get(16), "Gala program",      past2,  "0.00",    false),
            event("Startup Summit Belgrade",     L.get(16), "Konferencija",      fut1,   "0.00",    false),

            // Magacin (17) — savremena umetnost
            event("Savremeni Ples Magacin",      L.get(17), "Plesna predstava",  past2,  "1000.00", false),
            event("Eksperimentalna Muzika MKM",  L.get(17), "Eksperimentalna",   past1,  "800.00",  false),
            event("MKM Performans Art Noć",      L.get(17), "Performans",        today,  "900.00",  false),

            // Exit Petrovaradin (18) — festival
            event("Exit Festival 2025 – Dan 1",  L.get(18), "Festival",          past5,  "6000.00", false),
            event("Exit Festival 2025 – Dan 2",  L.get(18), "Festival",          past4,  "6000.00", false),
            event("Exit Festival 2025 – Dan 3",  L.get(18), "Festival",          past3,  "6000.00", false),
            event("Exit 2026 – Rani Prodajni",   L.get(18), "Festival",          fut3,   "5000.00", false),

            // Klub Mladih Kragujevac (19)
            event("KMK Techno Petak",            L.get(19), "Tehno",             past2,  "800.00",  false),
            event("KMK House Subota",            L.get(19), "House muzika",      past1,  "900.00",  false),
            event("KMK Elektronika Noć",         L.get(19), "Elektronska muzika",today,  "1000.00", false)
        ));
        log.info("DataSeeder: {} evenata.", saved.size());
        return saved;
    }

    private Event event(String name, Location loc, String type, LocalDateTime date, String price, boolean recurrent) {
        Event e = new Event();
        e.setName(name); e.setAddress(loc.getAddress()); e.setType(type);
        e.setDate(date); e.setLocation(loc); e.setPrice(new BigDecimal(price)); e.setRecurrent(recurrent);
        return e;
    }

    // ─── Review-i ─────────────────────────────────────────────────────────────

    private void seedReviews(List<Event> events, List<User> users) {
        // Komentari za rotaciju — raznovrsni
        String[][] comments = {
            {"Savršeno iskustvo, organizacija besprekorna!", "4", "5", "4", "5"},
            {"Odlična muzika, malo preglasno ali sve super.", "4", "3", "5", "4"},
            {"Jedan od boljih nastupa ove godine u gradu.", "5", "5", "5", "5"},
            {"Publika odlična, atmosfera fenomenalna!", "5", "4", "4", "5"},
            {"Prijatno iznenađenje, svakako dolazim ponovo.", "4", "4", "5", "4"},
            {"Solidan event, par tehničkih propusta ali OK.", "3", "3", "4", "3"},
            {"Legendarna večer, pamet stoji!", "5", "5", "5", "5"},
        };

        int count = 0;
        int commentIdx = 0;
        for (Event ev : events) {
            if (ev.getDate().isAfter(LocalDateTime.now())) continue;
            Location loc = ev.getLocation();

            for (int u = 0; u < Math.min(5, users.size()); u++) {
                String[] c = comments[commentIdx % comments.length];
                count += saveReview(users.get(u), ev, loc,
                        Integer.parseInt(c[1]), Integer.parseInt(c[2]),
                        Integer.parseInt(c[3]), Integer.parseInt(c[4]),
                        c[0]);
                commentIdx++;
            }
        }
        log.info("DataSeeder: {} review-a.", count);
    }

    private int saveReview(User user, Event ev, Location loc,
                           int perf, int sound, int venue, int overall, String comment) {
        if (reviewRepo.existsByUserIdAndEventId(user.getId(), ev.getId())) return 0;
        Review r = new Review();
        r.setUser(user); r.setEvent(ev); r.setLocation(loc);
        r.setPerformanceRating(perf); r.setSoundLightingRating(sound);
        r.setVenueRating(venue); r.setOverallRating(overall);
        r.setCommentText(comment); r.setEventCount(1);
        reviewRepo.save(r);
        return 1;
    }

    // ─── Ukupna ocena lokacije ────────────────────────────────────────────────

    private void updateTotalRatings(List<Location> locs) {
        for (Location loc : locs) {
            Double avg = reviewRepo.calculateAverageOverallRatingByLocationId(loc.getId());
            if (avg != null) { loc.setTotalRating(avg); locationRepo.save(loc); }
        }
    }

    // ─── PDF seed (uvijek se pokušava — idempotentno po pdfKey) ─────────────

    private void seedPdfsIfNeeded() {
        String bucket = minioProperties.getBucket().getPdfs();
        List<Location> locs = locationRepo.findAll();
        int added = 0;
        for (Location loc : locs) {
            if (loc.getPdfKey() != null && !loc.getPdfKey().isBlank()) continue;
            try {
                byte[] pdfBytes = generatePdf(loc.getName(), pdfContentFor(loc));
                String key = uploadPdfBytes(pdfBytes, bucket);
                loc.setPdfKey(key);
                locationRepo.save(loc);
                added++;
                log.info("DataSeeder: PDF dodat za '{}'.", loc.getName());
            } catch (Exception e) {
                log.warn("DataSeeder: PDF nije mogao biti dodat za '{}': {}", loc.getName(), e.getMessage());
            }
        }
        if (added > 0) log.info("DataSeeder: {} PDF-ova dodato.", added);
    }

    private String uploadPdfBytes(byte[] bytes, String bucket) throws Exception {
        String key = UUID.randomUUID() + ".pdf";
        try (ByteArrayInputStream in = new ByteArrayInputStream(bytes)) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket).object(key)
                    .stream(in, bytes.length, -1)
                    .contentType("application/pdf")
                    .build());
        }
        return key;
    }

    private byte[] generatePdf(String title, String body) throws Exception {
        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 15);
                cs.beginText();
                cs.newLineAtOffset(50, 800);
                cs.showText(toAscii(title));
                cs.endText();

                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 11);
                float y = 775;
                for (String line : body.split("\n")) {
                    if (y < 50) break;
                    for (String chunk : splitLine(toAscii(line), 90)) {
                        if (y < 50) break;
                        cs.beginText();
                        cs.newLineAtOffset(50, y);
                        cs.showText(chunk);
                        cs.endText();
                        y -= 16;
                    }
                }
            }
            doc.save(out);
            return out.toByteArray();
        }
    }

    private String toAscii(String s) {
        if (s == null) return "";
        return s
            .replace("č","c").replace("Č","C").replace("ć","c").replace("Ć","C")
            .replace("š","s").replace("Š","S").replace("ž","z").replace("Ž","Z")
            .replace("đ","d").replace("Đ","D").replace("dž","dz").replace("Dž","Dz")
            .replace("lj","lj").replace("nj","nj")
            // cirilica
            .replace("а","a").replace("б","b").replace("в","v").replace("г","g")
            .replace("д","d").replace("е","e").replace("ж","z").replace("з","z")
            .replace("и","i").replace("ј","j").replace("к","k").replace("л","l")
            .replace("м","m").replace("н","n").replace("о","o").replace("п","p")
            .replace("р","r").replace("с","s").replace("т","t").replace("у","u")
            .replace("ф","f").replace("х","h").replace("ц","c").replace("ч","c")
            .replace("ш","s").replace("ђ","d").replace("ћ","c").replace("љ","lj")
            .replace("њ","nj").replace("џ","dz")
            .replace("А","A").replace("Б","B").replace("В","V").replace("Г","G")
            .replace("Д","D").replace("Е","E").replace("Ж","Z").replace("З","Z")
            .replace("И","I").replace("Ј","J").replace("К","K").replace("Л","L")
            .replace("М","M").replace("Н","N").replace("О","O").replace("П","P")
            .replace("Р","R").replace("С","S").replace("Т","T").replace("У","U")
            .replace("Ф","F").replace("Х","H").replace("Ц","C").replace("Ч","C")
            .replace("Ш","S").replace("Ђ","D").replace("Ћ","C").replace("Љ","Lj")
            .replace("Њ","Nj").replace("Џ","Dz")
            .replace("–","-").replace("—","-");
    }

    private List<String> splitLine(String line, int max) {
        if (line.length() <= max) return List.of(line.isEmpty() ? " " : line);
        List<String> parts = new java.util.ArrayList<>();
        while (line.length() > max) {
            int cut = line.lastIndexOf(' ', max);
            if (cut <= 0) cut = max;
            parts.add(line.substring(0, cut));
            line = line.substring(cut).stripLeading();
        }
        if (!line.isBlank()) parts.add(line);
        return parts;
    }

    private String pdfContentFor(Location loc) {
        String name = loc.getName() != null ? loc.getName() : "";
        String type = loc.getType() != null ? loc.getType().toLowerCase() : "";

        String common =
            "\nOpšte informacije\n" +
            "Radno vreme: petkom i subotom od 22:00 do 06:00, ostali dani od 20:00 do 02:00.\n" +
            "Kontakt i rezervacije: info@" + name.toLowerCase().replaceAll("[^a-z0-9]", "") + ".rs\n" +
            "\nPristupacnost\n" +
            "Objekat je pristupačan osobama sa invaliditetom — rampa na ulazu, lift do svih etaža.\n" +
            "Garderobu nudi besplatno svim posetiocima.\n" +
            "Parking: besplatan parking za 150 vozila u neposrednoj blizini objekta.\n";

        if (type.contains("klub")) {
            return "Tehnički opis prostora\n" +
                "Kapacitet: do 1500 posetilaca u glavnoj sali, VIP zona za 100 osoba.\n" +
                "Bina: profesionalna pozornica dimenzija 12x8 metara, osvetljenost 2000 luksa.\n" +
                "Zvucni sistem: d&b audiotechnik system, 40.000 W ukupne snage, subwoofer linija.\n" +
                "Svetlosna oprema: moving head reflektori, laser sistem, stroboskopi, LED zid 6x4m.\n" +
                "Ventilacija: industrijski ventilacioni sistem, klima uređaji u svim prostorijama.\n" +
                "Tehnicka oprema: CDJ-3000 plejeri, Pioneer DJM-A9 mešetač, Allen&Heath mixer.\n" +
                common;
        }
        if (type.contains("jazz") || type.contains("bar")) {
            return "Tehnicki opis prostora\n" +
                "Kapacitet: do 200 posetilaca, intimna atmosfera sa stolovima i barskim stolicama.\n" +
                "Bina: mala bina 4x3 metra, savršena akustika za zivi nastup jazz i blues bendova.\n" +
                "Zvucni sistem: Bose profesionalni sistem, 5000 W, optimizovan za akusticne instrumente.\n" +
                "Muzicki instrumenti dostupni izvodjacima: Steinway klavir, Pearl bubanj setovi.\n" +
                "Vinska karta: preko 80 vrsta vina iz Srbije, Francuske i Italije.\n" +
                "Kujna: domaci specijaliteti, hrana dostupna do 01:00.\n" +
                common;
        }
        if (type.contains("koncert") || type.contains("arena") || type.contains("dvorana")) {
            return "Tehnicki opis prostora\n" +
                "Kapacitet: do 5000 posetilaca u sali sa sedistima, do 8000 stajace pozicije.\n" +
                "Glavna pozornica: 20x15 metara, nosivost do 10 tona opreme.\n" +
                "Zvucni sistem: L-Acoustics K1 line array, 120.000 W, DSP procesor.\n" +
                "Svetlosna oprema: grandMA3 upravljacki konzola, 300+ moving head jedinica, LED ekran 12x6m.\n" +
                "Backstage: 10 garderobera, producijski prostor, catering i medicinski punkt.\n" +
                "Streaming i snimanje: HD kamera set up, OB van prikljucak.\n" +
                common;
        }
        if (type.contains("festival") || type.contains("open air")) {
            return "Tehnicki opis prostora\n" +
                "Kapacitet: do 30.000 posetilaca na otvorenom prostoru.\n" +
                "Bine: tri bine — glavna (30x20m), sporednа (15x10m) i akusticna (8x6m).\n" +
                "Zvucni sistem: Meyer Sound LEO, 500.000 W ukupne snage po svim binama.\n" +
                "Generatori: diesel agregati 2000 kVA za potpunu energetsku nezavisnost.\n" +
                "Sanitarne jedinice: 200+ toaleta, tus kabine za izvodjace i VIP posetioce.\n" +
                "Sigurnost: 500 clanova obezbedenja, medicinska sluzba sa ambulantom.\n" +
                common;
        }
        if (type.contains("kulturni") || type.contains("centar")) {
            return "Tehnicki opis prostora\n" +
                "Kapacitet: vise sala — velika sala 500 mesta, mala sala 150 mesta, galerija 200 osoba.\n" +
                "Oprema za projekcije: 4K laser projektor, ekran 8x5 metara, Dolby Atmos zvuk.\n" +
                "Izlozbe: klima kontrola 20°C, UV zastitno osvetljenje za umetnicki materijal.\n" +
                "Konferencijska oprema: simultano prevodilacki sistemi za 5 jezika, HD videokonferencija.\n" +
                "Tehnicka podrska: stalni tehnicar na raspolaganju tokom svih dogadjaja.\n" +
                common;
        }
        // default
        return "Tehnicki opis prostora\n" +
            "Kapacitet: prilagodljiv prostor od 100 do 2000 posetilaca.\n" +
            "Bina i zvuk: profesionalna pozornica sa kompletnom audio i video opremom.\n" +
            "Zvucni sistem: visokokvalitetni PA sistem, optimizovan za sve vrste dogadjaja.\n" +
            "Svetlosna oprema: programabilni LED sistemi, moving head reflektori.\n" +
            "Tehnicka podrska: iskusan tehnicarski tim dostupan za sve dogadjaje.\n" +
            common;
    }
}
