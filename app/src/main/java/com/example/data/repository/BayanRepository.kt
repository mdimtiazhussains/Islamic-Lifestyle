package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.database.BayanDao
import com.example.data.database.LifestyleDatabase
import com.example.data.model.BayanClipEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * BayanRepository
 *
 * Exhaustive Non-Repeating Shuffle Engine for Islamic Reminders.
 * Sourced from https://www.nooresunnat.com/shortwhatsappbayan.php
 */
class BayanRepository(private val bayanDao: BayanDao) {

    companion object {
        private const val TAG = "BayanRepository"

        @Volatile
        private var INSTANCE: BayanRepository? = null

        fun getInstance(context: Context): BayanRepository {
            return INSTANCE ?: synchronized(this) {
                val db = LifestyleDatabase.getDatabase(context.applicationContext)
                val repo = BayanRepository(db.bayanDao())
                INSTANCE = repo
                repo
            }
        }

        val SEED_CLIPS = listOf(
            BayanClipEntity(
                id = "bayan_01",
                title = "Waaldain Ka Adab o Ehtiram",
                audioUrl = "https://www.nooresunnat.com/Audio/WhatsApp%20Clips/Video%20Files/Waaldain_Ka_Adab_o_Ehtiram.mp4",
                durationSec = 326,
                isPlayed = false,
                speaker = "Maulana Mehboob Elahi Sahab",
                category = "Family Life"
            ),
            BayanClipEntity(
                id = "bayan_02",
                title = "Ana Ko Chor Dain Ghar Ko Jannat Banae",
                audioUrl = "https://www.nooresunnat.com/Audio/WhatsApp%20Clips/Video%20Files/ana_ko_chor_dain_ghar_ko_jannat_banae.mp4",
                durationSec = 195,
                isPlayed = false,
                speaker = "Maulana Mehboob Elahi Sahab",
                category = "Family Peace"
            ),
            BayanClipEntity(
                id = "bayan_03",
                title = "Hamara Kaam Deen Ki Baat Batana Hai",
                audioUrl = "https://www.nooresunnat.com/Audio/WhatsApp%20Clips/Video%20Files/hamara_kaam_deen_ki_baat_batana_hai.mp4",
                durationSec = 165,
                isPlayed = false,
                speaker = "Maulana Mehboob Elahi Sahab",
                category = "Dawat o Tableegh"
            ),
            BayanClipEntity(
                id = "bayan_04",
                title = "La Ilmi Bahana Nahi",
                audioUrl = "https://www.nooresunnat.com/Audio/WhatsApp%20Clips/Video%20Files/La_Ilmi_bahana_nahi.mp4",
                durationSec = 140,
                isPlayed = false,
                speaker = "Maulana Mehboob Elahi Sahab",
                category = "Ilm e Deen"
            ),
            BayanClipEntity(
                id = "bayan_05",
                title = "Maafi Talafi Karlia Karo",
                audioUrl = "https://www.nooresunnat.com/Audio/WhatsApp%20Clips/Video%20Files/Maafi_Talafi_Karlia_Karo.mp4",
                durationSec = 180,
                isPlayed = false,
                speaker = "Maulana Mehboob Elahi Sahab",
                category = "Husn e Akhlaq"
            ),
            BayanClipEntity(
                id = "bayan_06",
                title = "Pooray Deen Main Dakhil Ho Jao",
                audioUrl = "https://www.nooresunnat.com/Audio/WhatsApp%20Clips/Video%20Files/Pooray_Deen_Main_Dakhil_Ho_Jao.mp4",
                durationSec = 210,
                isPlayed = false,
                speaker = "Maulana Mehboob Elahi Sahab",
                category = "Itaat e Ilahi"
            ),
            BayanClipEntity(
                id = "bayan_07",
                title = "Na Baligh Aulaad Kay Intiqaal Pe Sabr",
                audioUrl = "https://www.nooresunnat.com/Audio/WhatsApp%20Clips/Video%20Files/Na_Baligh_Aulaad_Kay_Intiqaal_Pe_Sabr.mp4",
                durationSec = 240,
                isPlayed = false,
                speaker = "Maulana Mehboob Elahi Sahab",
                category = "Sabr o Shukr"
            ),
            BayanClipEntity(
                id = "bayan_08",
                title = "Naimato Ka Sawal Hoga",
                audioUrl = "https://www.nooresunnat.com/Audio/WhatsApp%20Clips/Video%20Files/Naimato_Ka_Sawal_Hoga.mp4",
                durationSec = 190,
                isPlayed = false,
                speaker = "Maulana Mehboob Elahi Sahab",
                category = "Fikr e Aakhirat"
            ),
            BayanClipEntity(
                id = "bayan_09",
                title = "Rishtedari Ke Huqooq",
                audioUrl = "https://www.nooresunnat.com/Audio/WhatsApp%20Clips/Video%20Files/rishtedari_ke_huqooq.mp4",
                durationSec = 205,
                isPlayed = false,
                speaker = "Maulana Mehboob Elahi Sahab",
                category = "Sila Rehmi"
            ),
            BayanClipEntity(
                id = "bayan_10",
                title = "Mian Biwi Aik Dusre Ki Tareef Main Kanjusi Na Karain",
                audioUrl = "https://www.nooresunnat.com/Audio/WhatsApp%20Clips/Video%20Files/Mian_Biwi_Aik_Dusre_Ki_Tareef_Main_Kanjusi_Na_Karain.mp4",
                durationSec = 175,
                isPlayed = false,
                speaker = "Maulana Mehboob Elahi Sahab",
                category = "Zojain Ke Huqooq"
            ),
            BayanClipEntity(
                id = "bayan_11",
                title = "Khula Aur Talaq Ke Masail Ko Seekhain",
                audioUrl = "https://www.nooresunnat.com/Audio/WhatsApp%20Clips/Video%20Files/khula_aur_talaq_ke_masail_ko_seekhain.mp4",
                durationSec = 190,
                isPlayed = false,
                speaker = "Maulana Mehboob Elahi Sahab",
                category = "Masail e Shariat"
            ),
            BayanClipEntity(
                id = "bayan_12",
                title = "Taziyat Ka Sahi Tareeqa",
                audioUrl = "https://www.nooresunnat.com/Audio/WhatsApp%20Clips/Video%20Files/Taziyat_Ka_Sahi_Tareeqa.mp4",
                durationSec = 160,
                isPlayed = false,
                speaker = "Maulana Mehboob Elahi Sahab",
                category = "Sunnat Tareeqa"
            ),
            BayanClipEntity(
                id = "bayan_13",
                title = "Wirasat Kay Masail Seekhay Aur Jald Taqseem Karain",
                audioUrl = "https://www.nooresunnat.com/Audio/WhatsApp%20Clips/Video%20Files/Wirasat_k_masail_seekhay_aur_jald_taqseem_karain.mp4",
                durationSec = 220,
                isPlayed = false,
                speaker = "Maulana Mehboob Elahi Sahab",
                category = "Huqooq ul Ibad"
            ),
            BayanClipEntity(
                id = "bayan_14",
                title = "Baitullah Main Video Banane Se Guraiz",
                audioUrl = "https://www.nooresunnat.com/Audio/WhatsApp%20Clips/Video%20Files/Baitullah_main_video.mp4",
                durationSec = 155,
                isPlayed = false,
                speaker = "Maulana Mehboob Elahi Sahab",
                category = "Adab e Haram"
            ),
            BayanClipEntity(
                id = "bayan_15",
                title = "Ijtimaee Maal Main Khayanat Na Karain",
                audioUrl = "https://www.nooresunnat.com/Audio/WhatsApp%20Clips/Video%20Files/ijtimaee%20maal%20main%20khayanat.mp4",
                durationSec = 185,
                isPlayed = false,
                speaker = "Maulana Mehboob Elahi Sahab",
                category = "Amanatdari"
            ),
            BayanClipEntity(
                id = "bayan_16",
                title = "Dua e Janaza Aur Mayyat Ki Bakhshish",
                audioUrl = "https://www.nooresunnat.com/Audio/WhatsApp%20Clips/Video%20Files/Dua_e_Janaza.mp4",
                durationSec = 150,
                isPlayed = false,
                speaker = "Maulana Mehboob Elahi Sahab",
                category = "Dua o Astaghfar"
            ),
            BayanClipEntity(
                id = "bayan_17",
                title = "Dukan Ke Agay Jaga Gherna Aur Huqooq ul Ibad",
                audioUrl = "https://www.nooresunnat.com/Audio/WhatsApp%20Clips/Video%20Files/dukan_ke_agay_jaga_gherna.mp4",
                durationSec = 170,
                isPlayed = false,
                speaker = "Maulana Mehboob Elahi Sahab",
                category = "Tijarat o Huqooq"
            ),
            BayanClipEntity(
                id = "bayan_18",
                title = "Masjid Main Dunyawi Bato Ka Gunah",
                audioUrl = "https://www.nooresunnat.com/Audio/WhatsApp%20Clips/Video%20Files/Masjid_main_dunyawi_bato_ka_gunah.mp4",
                durationSec = 165,
                isPlayed = false,
                speaker = "Maulana Mehboob Elahi Sahab",
                category = "Adab e Masjid"
            ),
            BayanClipEntity(
                id = "bayan_19",
                title = "Narmi Ikhtiyar Karo Aur Gussa Piyo",
                audioUrl = "https://www.nooresunnat.com/Audio/WhatsApp%20Clips/Video%20Files/narmi_ikhtiyar_karo.mp4",
                durationSec = 180,
                isPlayed = false,
                speaker = "Maulana Mehboob Elahi Sahab",
                category = "Husn e Akhlaq"
            ),
            BayanClipEntity(
                id = "bayan_20",
                title = "Namaz Ki Pabandi Aur Dil Ka Sukoon",
                audioUrl = "https://www.nooresunnat.com/Audio/WhatsApp%20Clips/Video%20Files/whatsapp%20clip5.mp4",
                durationSec = 120,
                isPlayed = false,
                speaker = "Maulana Mehboob Elahi Sahab",
                category = "Namaz Reminder"
            ),
            BayanClipEntity(
                id = "bayan_21",
                title = "Gheebat Aur Chughli Se Bachne Ka Tariqa",
                audioUrl = "https://www.nooresunnat.com/Audio/WhatsApp%20Clips/Video%20Files/whatsapp%20clip10a.mp4",
                durationSec = 135,
                isPlayed = false,
                speaker = "Maulana Mehboob Elahi Sahab",
                category = "Hifazat e Zaban"
            ),
            BayanClipEntity(
                id = "bayan_22",
                title = "Tahajjud Ki Fazeelat Aur Rona",
                audioUrl = "https://www.nooresunnat.com/Audio/WhatsApp%20Clips/Video%20Files/whatsapp%20clip11.mp4",
                durationSec = 145,
                isPlayed = false,
                speaker = "Maulana Mehboob Elahi Sahab",
                category = "Qiyam ul Lail"
            ),
            BayanClipEntity(
                id = "bayan_23",
                title = "Tauba Aur Astaghfar Ki Taqat",
                audioUrl = "https://www.nooresunnat.com/Audio/WhatsApp%20Clips/Video%20Files/whatsapp%20clip4.mp4",
                durationSec = 130,
                isPlayed = false,
                speaker = "Maulana Mehboob Elahi Sahab",
                category = "Tauba"
            ),
            BayanClipEntity(
                id = "bayan_24",
                title = "Rizq e Halal Ki Barkat",
                audioUrl = "https://www.nooresunnat.com/Audio/WhatsApp%20Clips/Video%20Files/whatsapp%20clip16a.mp4",
                durationSec = 140,
                isPlayed = false,
                speaker = "Maulana Mehboob Elahi Sahab",
                category = "Halal Kamai"
            ),
            BayanClipEntity(
                id = "bayan_25",
                title = "Husn e Akhlaq Aur Narmi",
                audioUrl = "https://www.nooresunnat.com/Audio/WhatsApp%20Clips/Video%20Files/whatsapp%20clip2.mp4",
                durationSec = 125,
                isPlayed = false,
                speaker = "Maulana Mehboob Elahi Sahab",
                category = "Muashrat"
            ),
            BayanClipEntity(
                id = "bayan_26",
                title = "Maut Ki Yaad Aur Aakhirat Ki Fikr",
                audioUrl = "https://www.nooresunnat.com/Audio/WhatsApp%20Clips/Video%20Files/whatsapp%20clip7.mp4",
                durationSec = 150,
                isPlayed = false,
                speaker = "Maulana Mehboob Elahi Sahab",
                category = "Aakhirat"
            ),
            BayanClipEntity(
                id = "bayan_27",
                title = "Sabr Aur Shukr Ki Fazilat",
                audioUrl = "https://www.nooresunnat.com/Audio/WhatsApp%20Clips/Video%20Files/whatsapp%20clip8.mp4",
                durationSec = 135,
                isPlayed = false,
                speaker = "Maulana Mehboob Elahi Sahab",
                category = "Islah e Dil"
            ),
            BayanClipEntity(
                id = "bayan_28",
                title = "Dua Ki Qubooliyat Ke Auqat",
                audioUrl = "https://www.nooresunnat.com/Audio/WhatsApp%20Clips/Video%20Files/whatsapp%20clip9.mp4",
                durationSec = 140,
                isPlayed = false,
                speaker = "Maulana Mehboob Elahi Sahab",
                category = "Dua"
            ),
            BayanClipEntity(
                id = "bayan_29",
                title = "Sunnat e Rasool Par Amal",
                audioUrl = "https://www.nooresunnat.com/Audio/WhatsApp%20Clips/Video%20Files/whatsapp%20clip14.mp4",
                durationSec = 130,
                isPlayed = false,
                speaker = "Maulana Mehboob Elahi Sahab",
                category = "Ittiba e Sunnat"
            ),
            BayanClipEntity(
                id = "bayan_30",
                title = "Sadqah Ki Barkat Aur Museebat Se Hifazat",
                audioUrl = "https://www.nooresunnat.com/Audio/WhatsApp%20Clips/Video%20Files/whatsapp%20clip15.mp4",
                durationSec = 125,
                isPlayed = false,
                speaker = "Maulana Mehboob Elahi Sahab",
                category = "Infaq Fi Sabeelillah"
            ),
            BayanClipEntity(
                id = "bayan_31",
                title = "Qurani Tilawat Aur Ghar Ka Aman",
                audioUrl = "https://www.nooresunnat.com/Audio/WhatsApp%20Clips/Video%20Files/whatsapp%20clip6.mp4",
                durationSec = 140,
                isPlayed = false,
                speaker = "Maulana Mehboob Elahi Sahab",
                category = "Tilawat e Quran"
            ),
            BayanClipEntity(
                id = "bayan_32",
                title = "Bhai Charagi Aur Dilon Ki Safai",
                audioUrl = "https://www.nooresunnat.com/Audio/WhatsApp%20Clips/Video%20Files/whatsapp%20clip12a.mp4",
                durationSec = 135,
                isPlayed = false,
                speaker = "Maulana Mehboob Elahi Sahab",
                category = "Ukhuwwat"
            ),
            BayanClipEntity(
                id = "bayan_33",
                title = "Zikrullah Aur Roohani Sukoon",
                audioUrl = "https://www.nooresunnat.com/Audio/WhatsApp%20Clips/Video%20Files/whatsapp%20clip3.mp4",
                durationSec = 150,
                isPlayed = false,
                speaker = "Maulana Mehboob Elahi Sahab",
                category = "Zikr e Ilahi"
            )
        )
    }

    /**
     * Seeds initial collection of authentic short bayans into database if empty.
     */
    suspend fun checkAndSeedInitialClips() = withContext(Dispatchers.IO) {
        val count = bayanDao.getClipCount()
        if (count == 0) {
            Log.i(TAG, "Seeding ${SEED_CLIPS.size} initial bayan clips from nooresunnat.com into bayan_clips")
            bayanDao.insertClips(SEED_CLIPS)
        }
    }

    /**
     * Exhaustive Non-Repeating Shuffle Engine:
     * 1. Query unplayed clips (isPlayed == false).
     * 2. If unplayed pool is exhausted, reset all clips to isPlayed = false, increment cycle,
     *    and re-query.
     * 3. Pick a pseudo-random clip from the unplayed pool so no clip repeats until all have been heard.
     */
    suspend fun getNextUnplayedClip(): BayanClipEntity = withContext(Dispatchers.IO) {
        checkAndSeedInitialClips()

        var unplayed = bayanDao.getUnplayedClips()
        if (unplayed.isEmpty()) {
            Log.i(TAG, "All clips played in current rotation cycle. Auto-resetting rotation cycle...")
            bayanDao.resetAllClips()
            unplayed = bayanDao.getUnplayedClips()
        }

        if (unplayed.isEmpty()) {
            // Fallback safety
            SEED_CLIPS.first()
        } else {
            unplayed.random()
        }
    }

    suspend fun markClipPlayed(id: String) = withContext(Dispatchers.IO) {
        bayanDao.markClipPlayed(id, true)
    }

    suspend fun resetAllClips() = withContext(Dispatchers.IO) {
        bayanDao.resetAllClips()
    }

    fun getAllClips(): Flow<List<BayanClipEntity>> = bayanDao.getAllClips()
}
