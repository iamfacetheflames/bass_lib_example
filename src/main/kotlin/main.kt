
import jouvieje.bass.Bass
import jouvieje.bass.BassInit
import jouvieje.bass.defines.BASS_SAMPLE
import jouvieje.bass.exceptions.BassException
import jouvieje.bass.structures.HSAMPLE
import java.io.File

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
        val sam: HSAMPLE = Bass.BASS_SampleLoad(false, testAudioFile.absolutePath, 0, 0, 3, BASS_SAMPLE.BASS_SAMPLE_OVER_POS)
        val channel = Bass.BASS_SampleGetChannel(sam, false)
        if (!Bass.BASS_ChannelPlay(channel.asInt(), false)) {
            error("Can't play sample")
        }
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