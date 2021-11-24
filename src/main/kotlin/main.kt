
import jouvieje.bass.Bass
import jouvieje.bass.BassInit
import jouvieje.bass.defines.BASS_POS.BASS_POS_MUSIC_ORDER
import jouvieje.bass.defines.BASS_SAMPLE
import jouvieje.bass.exceptions.BassException
import jouvieje.bass.structures.HSAMPLE
import java.io.File
import jouvieje.bass.Bass.BASS_ChannelSeconds2Bytes
import jouvieje.bass.defines.BASS_ATTRIB.BASS_ATTRIB_VOL
import jouvieje.bass.defines.BASS_POS.BASS_POS_BYTE


var init: Boolean = false

fun main(args: Array<String>) {
    println("Hello World!")

    init()

    if (!init) {
        return
    }

    // check the correct BASS was loaded
    if (Bass.BASS_GetVersion() and -0x10000 shr 16 != BassInit.BASSVERSION()) {
        println("An incorrect version of BASS.DLL was loaded")
        return
    }

    /* Initialize default output device */
    if (
        !Bass.BASS_Init(
            -1,
            44100,
            0,
            null,
            null
        )
    ) {
        error("Can't initialize device")
    } else {
        val testAudioFile = File( "test.mp3")
//        Bass.BASS_SetVolume(1f)
        val handle: HSAMPLE = Bass.BASS_SampleLoad(false, testAudioFile.absolutePath, 0, 0, 3, BASS_SAMPLE.BASS_SAMPLE_OVER_POS)
        val channel = Bass.BASS_SampleGetChannel(handle, false)
        if (!Bass.BASS_ChannelPlay(channel.asInt(), false)) {
            error("Can't play sample")
        }

        // seek to 8.7 sec
        val prebuf = BASS_ChannelSeconds2Bytes(channel.asInt(), 8.7)
        Bass.BASS_ChannelSetPosition(channel.asInt(), prebuf, BASS_POS_BYTE)

        // set volume to 0.5f for 15 sec
        Bass.BASS_ChannelSlideAttribute(channel.asInt(), BASS_ATTRIB_VOL, 0.5f, 15000)
    }
    readLine()
}

fun init() {
    /*
		 * NativeBass Init
		 */
    try {
        BassInit.loadLibraries()
    } catch (e: BassException) {
        println("NativeBass error! %s\n ${e.message}")
        return
    }

    if (BassInit.NATIVEBASS_LIBRARY_VERSION() != BassInit.NATIVEBASS_JAR_VERSION()) {
        println(
            "Error!  NativeBass library version (%08x) is different to jar version (%08x)\n" +
                    "${            
                        BassInit.NATIVEBASS_LIBRARY_VERSION()
                    } \n" +
                    "${
                        BassInit.NATIVEBASS_JAR_VERSION()
                    }"
        )
        return
    }
    init = true
}