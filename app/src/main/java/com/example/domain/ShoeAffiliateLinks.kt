package com.example.domain

import android.net.Uri

/**
 * Rich specification and Shopee affiliate link registry for all curated StrideApex shoes.
 */
data class ShoeDetailExtra(
    val shopeeUrl: String,
    val estimatedPriceIdr: String,
    val cushionLevel: String,
    val surfaceType: String,
    val sizingTip: String,
    val pros: List<String>
)

/**
 * DAFTAR TAUTAN AFFILIATE SHOPEE STRIDEAPEX (20 SEPATU)
 * ------------------------------------------------------------------
 * Cara mengganti dengan tautan Shopee Affiliate Anda:
 * Ganti nilai `shopeeUrl` di bawah menggunakan link pendek Shopee
 * Affiliate Anda (contoh: "https://s.shopee.co.id/xxxxxx").
 * Secara default mengarah ke pencarian produk resmi di Shopee.
 */
object ShoeAffiliateLinks {

    fun slugify(brand: String, name: String): String {
        return "$brand $name"
            .lowercase()
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
    }

    val CATALOG: Map<String, ShoeDetailExtra> = mapOf(
        "nike-vomero-plus" to ShoeDetailExtra(
            shopeeUrl = "https://shopee.co.id/search?keyword=Nike+Vomero+Plus",
            estimatedPriceIdr = "Rp 2.599.000",
            cushionLevel = "Maximalist · Full-Length ZoomX",
            surfaceType = "Aspal / Road",
            sizingTip = "True to size — ruang jari cukup lega untuk pembengkakan kaki saat long run.",
            pros = listOf(
                "Busa full-length ZoomX menjaga kaki tetap segar saat pemulihan",
                "Upper empuk dan konstruksi tahan pakai untuk volume mingguan tinggi",
                "Transisi tumit ke ujung kaki halus di pace easy hingga steady"
            )
        ),
        "nike-vomero-18" to ShoeDetailExtra(
            shopeeUrl = "https://shopee.co.id/search?keyword=Nike+Vomero+18",
            estimatedPriceIdr = "Rp 2.399.000",
            cushionLevel = "Max Cushion · ZoomX & ReactX",
            surfaceType = "Aspal / Road",
            sizingTip = "True to size — cocok untuk pelari bertipe kaki normal hingga sedikit lebar.",
            pros = listOf(
                "Drop tinggi 13.9 mm efektif mengurangi beban pada tendon Achilles dan betis",
                "Step-in comfort sangat empuk untuk pelari heel striker",
                "Platform stabil untuk lari santai harian jarak menengah hingga jauh"
            )
        ),
        "hoka-clifton-10" to ShoeDetailExtra(
            shopeeUrl = "https://shopee.co.id/search?keyword=HOKA+Clifton+10",
            estimatedPriceIdr = "Rp 2.599.000",
            cushionLevel = "Balanced Max · Supercritical EVA",
            surfaceType = "Aspal / Road",
            sizingTip = "True to size — tersedia toebox lebih lega dibanding generasi sebelumnya.",
            pros = listOf(
                "Stack 44.4 mm meredam benturan dengan bobot tetap terkontrol (275 g)",
                "Geometri Meta-Rocker membantu ayunan langkah mulus saat membangun base aerobik",
                "Kestabilan platform solid di tikungan maupun jalan basah"
            )
        ),
        "hoka-bondi-9" to ShoeDetailExtra(
            shopeeUrl = "https://shopee.co.id/search?keyword=HOKA+Bondi+9",
            estimatedPriceIdr = "Rp 2.799.000",
            cushionLevel = "Ultra Plush · Supercritical EVA",
            surfaceType = "Aspal / Road & Treadmill",
            sizingTip = "True to size — disarankan memakai kaus kaki lari ketebalan sedang.",
            pros = listOf(
                "Lebih ringan dan responsif dibanding generasi sebelumnya dengan upper premium",
                "Perlindungan sendi lutut & pergelangan kaki kelas atas untuk recovery day",
                "Ketahanan outsole sangat baik untuk pemakaian harian maupun jalan kaki jauh"
            )
        ),
        "adidas-adizero-evo-sl" to ShoeDetailExtra(
            shopeeUrl = "https://shopee.co.id/search?keyword=adidas+Adizero+EVO+SL",
            estimatedPriceIdr = "Rp 2.500.000",
            cushionLevel = "Responsive Superfoam · Lightstrike Pro",
            surfaceType = "Aspal / Road & Track",
            sizingTip = "True to size — bagian midfoot agak ramping khas lini Adizero.",
            pros = listOf(
                "100% busa balap Lightstrike Pro dengan bobot ultra-ringan hanya 223 g",
                "Fleksibel dipakai dari lari santai hingga sesi tempo & fartlek cepat",
                "Outsole karet Continental memberi traksi unggul di jalan kering maupun basah"
            )
        ),
        "asics-novablast-5" to ShoeDetailExtra(
            shopeeUrl = "https://shopee.co.id/search?keyword=ASICS+Novablast+5",
            estimatedPriceIdr = "Rp 2.299.000",
            cushionLevel = "Bouncy Responsive · FF Blast+ ECO",
            surfaceType = "Aspal / Road",
            sizingTip = "True to size — ukuran standar ASICS terasa pas tanpa perlu naik size.",
            pros = listOf(
                "Pantulan busa FF Blast+ ECO hidup dan ringan (254 g)",
                "Lebih stabil untuk lari steady jarak menengah hingga half marathon training",
                "Upper engineered mesh terasa sejuk di iklim tropis"
            )
        ),
        "hoka-mach-7" to ShoeDetailExtra(
            shopeeUrl = "https://shopee.co.id/search?keyword=HOKA+Mach+7",
            estimatedPriceIdr = "Rp 2.499.000",
            cushionLevel = "Firm Responsive · Supercritical EVA",
            surfaceType = "Aspal / Road & Track",
            sizingTip = "True to size — fit snug berorientasi kecepatan; naikkan 0.5 size jika kaki sangat lebar.",
            pros = listOf(
                "Bobot lincah 224 g memudahkan peningkatan cadence >175 spm",
                "Tanpa pelat karbon sehingga kaki tetap bisa melatih kekuatan otot intrinsik",
                "Outsole lebih tahan gesek dibanding seri Mach lawas"
            )
        ),
        "new-balance-supercomp-rebel" to ShoeDetailExtra(
            shopeeUrl = "https://shopee.co.id/search?keyword=New+Balance+FuelCell+Rebel",
            estimatedPriceIdr = "Rp 2.599.000",
            cushionLevel = "Light Propulsive · FuelCell",
            surfaceType = "Aspal / Road & Track",
            sizingTip = "True to size atau naik 0.5 size jika menyukai ruang ekstra di depan.",
            pros = listOf(
                "Bobot sangat ringan 207 g dengan respons FuelCell eksplosif",
                "Cocok untuk speed play, interval 5K–10K, maupun persiapan lomba",
                "Daya cengkeram outsole mumpuni saat akselerasi cepat"
            )
        ),
        "asics-megablast" to ShoeDetailExtra(
            shopeeUrl = "https://shopee.co.id/search?keyword=ASICS+Megablast",
            estimatedPriceIdr = "Rp 3.499.000",
            cushionLevel = "High-Stack Superfoam · FF Turbo",
            surfaceType = "Aspal / Road",
            sizingTip = "True to size — penguncian tumit mantap meski stack midsole setebal 45.1 mm.",
            pros = listOf(
                "Energy return sangat tinggi di bobot hanya 218 g tanpa terasa goyah",
                "Peredaman benturan superior untuk sesi long fast run di atas 20 km",
                "Traksi kelas atas di kondisi aspal basah sekalipun"
            )
        ),
        "asics-superblast-3" to ShoeDetailExtra(
            shopeeUrl = "https://shopee.co.id/search?keyword=ASICS+Superblast+3",
            estimatedPriceIdr = "Rp 3.399.000",
            cushionLevel = "Max Super-Trainer · FF LEAP",
            surfaceType = "Aspal / Road",
            sizingTip = "True to size — toebox lapang dan upper nyaman untuk lari berjam-jam.",
            pros = listOf(
                "Stack tebal 45.8 mm berbusa FF LEAP menjaga otot betis tetap segar di threshold pace",
                "Bobot turun ke 235 g membuat transisi pace cepat terasa lebih ringan",
                "Stabil di kecepatan lambat maupun marathon pace tanpa pelat karbon"
            )
        ),
        "saucony-endorphin-speed-5" to ShoeDetailExtra(
            shopeeUrl = "https://shopee.co.id/search?keyword=Saucony+Endorphin+Speed+5",
            estimatedPriceIdr = "Rp 2.899.000",
            cushionLevel = "Propulsive Nylon · PWRRUN PB",
            surfaceType = "Aspal / Road & Track",
            sizingTip = "True to size — lockdown midfoot presisi dan upper sangat adem.",
            pros = listOf(
                "Pelat nilon bersayap + busa PWRRUN PB memberi dorongan agresif tanpa kekakuan ekstrem",
                "SpeedRoll geometry memuluskan transisi saat latihan lactate threshold",
                "Serbaguna dipakai untuk tempo harian hingga race day Half Marathon"
            )
        ),
        "asics-superblast-2" to ShoeDetailExtra(
            shopeeUrl = "https://shopee.co.id/search?keyword=ASICS+Superblast+2",
            estimatedPriceIdr = "Rp 3.299.000",
            cushionLevel = "Max Responsive · FF Turbo+ / Blast+",
            surfaceType = "Aspal / Road",
            sizingTip = "True to size — pas untuk mayoritas bentuk kaki pelari.",
            pros = listOf(
                "Keseimbangan ideal antara pantulan bertenaga dan durabilitas outsole tinggi",
                "Melindungi kaki saat high mileage minggu persiapan marathon",
                "Platform lebar memberikan rasa percaya diri saat kaki mulai lelah"
            )
        ),
        "nike-zoom-fly-6" to ShoeDetailExtra(
            shopeeUrl = "https://shopee.co.id/search?keyword=Nike+Zoom+Fly+6",
            estimatedPriceIdr = "Rp 2.599.000",
            cushionLevel = "Carbon Plated · ZoomX + SR-02",
            surfaceType = "Aspal / Road",
            sizingTip = "True to size — bentuk race-trainer rapi; pertimbangkan +0.5 size untuk kaki lebar.",
            pros = listOf(
                "Kombinasi busa ZoomX + pelat karbon penuh memberi sensasi super-shoe untuk latihan",
                "Lebih tahan lama dan ekonomis dibanding super-shoe lomba murni",
                "Efektif untuk latihan threshold interval dan pace marathon"
            )
        ),
        "new-balance-fuelcell-supercomp-trainer-v3" to ShoeDetailExtra(
            shopeeUrl = "https://shopee.co.id/search?keyword=New+Balance+SuperComp+Trainer+v3",
            estimatedPriceIdr = "Rp 3.199.000",
            cushionLevel = "Stable Carbon · FuelCell",
            surfaceType = "Aspal / Road",
            sizingTip = "True to size — fit nyaman di bagian depan kaki dengan heel collar suportif.",
            pros = listOf(
                "Salah satu trainer berpelat karbon paling stabil untuk long run berkecepatan tinggi",
                "Geometry Energy Arc menghasilkan transisi bertenaga tanpa mengorbankan kenyamanan",
                "Outsole grip kuat menunjang volume latihan mingguan yang padat"
            )
        ),
        "nike-vaporfly-4" to ShoeDetailExtra(
            shopeeUrl = "https://shopee.co.id/search?keyword=Nike+Vaporfly+4",
            estimatedPriceIdr = "Rp 3.899.000",
            cushionLevel = "Elite Race · ZoomX + Flyplate",
            surfaceType = "Aspal / Road Race",
            sizingTip = "Fit lomba presisi — pilih ukuran pas lari atau +0.5 size untuk jarak Half Marathon.",
            pros = listOf(
                "Sangat ringan di 166 g dengan respons langsung untuk memecahkan PR 5K–HM",
                "Penyempurnaan lidah & upper mengunci kaki aman tanpa tekanan berlebih",
                "Transisi forefoot agresif mendorong running economy ke level puncak"
            )
        ),
        "adidas-adizero-adios-pro-4" to ShoeDetailExtra(
            shopeeUrl = "https://shopee.co.id/search?keyword=adidas+Adizero+Adios+Pro+4",
            estimatedPriceIdr = "Rp 4.000.000",
            cushionLevel = "Elite Race · Lightstrike Pro + EnergyRods",
            surfaceType = "Aspal / Road Race",
            sizingTip = "Naik 0.5 size bila ruang jari terasa rapat, terutama untuk target Full Marathon.",
            pros = listOf(
                "EnergyRods karbon + Lightstrike Pro lebih empuk menghasilkan dorongan eksplosif",
                "Bobot tepat 200 g dengan rocker depan tajam untuk menjaga kecepatan di km 30–42",
                "Outsole Lighttraxion + Continental memberi cengkeraman prima saat tikungan cepat"
            )
        ),
        "saucony-endorphin-pro-5" to ShoeDetailExtra(
            shopeeUrl = "https://shopee.co.id/search?keyword=Saucony+Endorphin+Pro+5",
            estimatedPriceIdr = "Rp 3.699.000",
            cushionLevel = "Stable Race · PWRRUN PB + Carbon",
            surfaceType = "Aspal / Road Race",
            sizingTip = "True to size — akomodatif untuk berbagai bentuk kaki saat race day.",
            pros = listOf(
                "Super-shoe berpelat karbon paling stabil untuk heel maupun midfoot striker",
                "Upper sangat sejuk dan nyaman dipakai tanpa lecet sepanjang 42.2 km",
                "Grip andal di kondisi jalan licin atau water station lomba"
            )
        ),
        "nike-alphafly-4" to ShoeDetailExtra(
            shopeeUrl = "https://shopee.co.id/search?keyword=Nike+Alphafly+4",
            estimatedPriceIdr = "Rp 4.399.000",
            cushionLevel = "Pinnacle Race · ZoomX + Air Pods + Carbon",
            surfaceType = "Aspal / Road Marathon",
            sizingTip = "True to size — cek bagian arch terlebih dulu jika memiliki telapak sangat datar (flat foot).",
            pros = listOf(
                "Dual Air Zoom Pods + full ZoomX + Flyplate menghadirkan propulsi rekor dunia",
                "Lebih ringan (190 g) dan Atomknit upper memberi lock-down teraman di pace <4:00/km",
                "Efisiensi benturan luar biasa untuk menjaga kesegaran otot kuadrisep hingga garis finis"
            )
        ),
        "asics-metaspeed-sky-tokyo" to ShoeDetailExtra(
            shopeeUrl = "https://shopee.co.id/search?keyword=ASICS+Metaspeed+Sky+Tokyo",
            estimatedPriceIdr = "Rp 4.299.000",
            cushionLevel = "Pinnacle Race · FF LEAP + Carbon",
            surfaceType = "Aspal / Road Marathon",
            sizingTip = "True to size — profil ramping aerodinamis untuk pelari bertipe stride (memanjangkan langkah).",
            pros = listOf(
                "Konstruksi super ringan 163 g membantu akselerasi cepat di setiap kilometer",
                "Busa FF LEAP membal bersamaan dengan pelat karbon penuh di telapak depan",
                "Sirkulasi udara upper sangat optimal menghadapi race bersuhu panas"
            )
        ),
        "adidas-adizero-adios-pro-evo-3" to ShoeDetailExtra(
            shopeeUrl = "https://shopee.co.id/search?keyword=adidas+Adizero+Adios+Pro+Evo",
            estimatedPriceIdr = "Rp 8.000.000",
            cushionLevel = "Hyper-Light Pinnacle · Lightstrike Pro Evo",
            surfaceType = "Aspal / Championship Race",
            sizingTip = "True to size — fit race-spec ekstrem khusus hari lomba & upaya rekor personal.",
            pros = listOf(
                "Bobot ultra-ekstrem ~99 g memangkas energi ayunan kaki secara drastis di pace sub-4:00/km",
                "Stack 38.1 mm dengan geometri forefoot rocker agresif untuk efisiensi puncak",
                "Dirancang khusus untuk mengejar podium dan pemecahan rekor kategori elit"
            )
        )
    )

    fun getDetailExtra(shoe: ShoeModel): ShoeDetailExtra {
        val slug = shoe.slug.ifBlank { slugify(shoe.brand, shoe.name) }
        return CATALOG[slug] ?: ShoeDetailExtra(
            shopeeUrl = "https://shopee.co.id/search?keyword=${Uri.encode("${shoe.brand} ${shoe.name}")}",
            estimatedPriceIdr = "Cek harga terbaru di Shopee",
            cushionLevel = "Performance Running Foam",
            surfaceType = "Aspal / Road",
            sizingTip = "True to size — sesuaikan dengan kaus kaki lari favorit Anda.",
            pros = listOf(
                "Terpilih dalam kurasi kasta StrideApex berdasarkan zona pace pelari",
                "Dirancang untuk efisiensi langkah dan perlindungan benturan optimal"
            )
        )
    }

    fun getShopeeUrl(shoe: ShoeModel): String {
        val overrideUrl = shoe.shopeeAffiliateUrl?.trim()
        if (!overrideUrl.isNullOrEmpty()) return overrideUrl
        return getDetailExtra(shoe).shopeeUrl
    }
}
