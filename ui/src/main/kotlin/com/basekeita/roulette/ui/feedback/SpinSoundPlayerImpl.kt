package com.basekeita.roulette.ui.feedback

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.SoundPool
import android.os.SystemClock
import com.basekeita.roulette.ui.R
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

/** クリック音の音量。密に重なっても耳に刺さらないところまで下げてある。 */
private const val CLICK_GAIN = 0.7f

private const val BYTES_PER_FLOAT = 4

/**
 * スピン中の回転音は、`ClickTrack` で 1 本の波形にしてから `AudioTrack`（`MODE_STATIC`、`PCM_FLOAT`）で一度に流し、リズムを揺らさない。
 * 指で動かしている間の 1 回ずつの音は `SoundPool` で鳴らす。オーディオフォーカスは取らない（他のアプリの音を止めない）。
 * 素材が読めない・オーディオが使えないときは黙って何もしない。
 */
@Singleton
internal class SpinSoundPlayerImpl
@Inject
constructor(
    @ApplicationContext private val context: Context,
) : SpinSoundPlayer {
    private val audioAttributes =
        AudioAttributes
            .Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

    /** 波形の組み立てと `AudioTrack` の用意は重いので、画面の処理とは別のスレッドで行う。 */
    private val audioExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    /** `stop()` や次の `play` のあとに、待っていた古い再生が始まらないようにする。 */
    private val generation = AtomicInteger(0)

    // 以下は audioExecutor のスレッドだけで触る
    private var click: WavDecoder.Decoded? = null
    private var currentTrack: AudioTrack? = null

    private var soundPool: SoundPool? = null

    /** `SoundPool` に読み込んだ音の id。読み込みが終わるまでは null。 */
    @Volatile
    private var loadedClickSoundId: Int? = null

    override fun prepare() {
        if (soundPool == null) {
            val pool = SoundPool.Builder().setMaxStreams(2).setAudioAttributes(audioAttributes).build()
            pool.setOnLoadCompleteListener { _, soundId, status -> if (status == 0) loadedClickSoundId = soundId }
            pool.load(context, R.raw.click, 1)
            soundPool = pool
        }
        audioExecutor.execute { loadClick() }
    }

    override fun play(timesSeconds: List<Double>) {
        val current = generation.incrementAndGet()
        if (timesSeconds.isEmpty() || !canPlay()) return

        val requestedAt = SystemClock.elapsedRealtime()
        audioExecutor.execute {
            if (generation.get() != current) return@execute

            val loaded = loadClick() ?: return@execute
            // 準備を待った分だけ遅れて鳴り始めるので、時刻をずらして過ぎたものは捨てる
            val waitedSeconds = (SystemClock.elapsedRealtime() - requestedAt) / 1_000.0
            val remaining = timesSeconds.map { time -> time - waitedSeconds }.filter { time -> time >= 0 }
            val track = ClickTrack.render(loaded.samples, loaded.sampleRate, remaining, CLICK_GAIN)
            startTrack(track, loaded.sampleRate)
        }
    }

    override fun playClick() {
        if (!canPlay()) return

        val soundId = loadedClickSoundId
        if (soundId == null) {
            prepare()
            return
        }
        soundPool?.play(soundId, CLICK_GAIN, CLICK_GAIN, 1, 0, 1f)
    }

    override fun stop() {
        generation.incrementAndGet()
        audioExecutor.execute { releaseCurrentTrack() }
    }

    /** 着信音モードが通常のときだけ鳴らす。 */
    private fun canPlay(): Boolean {
        val audioManager = context.getSystemService(AudioManager::class.java) ?: return false
        return audioManager.ringerMode == AudioManager.RINGER_MODE_NORMAL
    }

    private fun loadClick(): WavDecoder.Decoded? {
        click?.let { return it }

        val bytes = runCatching { context.resources.openRawResource(R.raw.click).use { stream -> stream.readBytes() } }.getOrNull()
        val decoded = bytes?.let(WavDecoder::decode)
        click = decoded
        return decoded
    }

    private fun startTrack(
        samples: FloatArray,
        sampleRate: Int,
    ) {
        releaseCurrentTrack()
        if (samples.isEmpty()) return

        val track =
            runCatching {
                AudioTrack
                    .Builder()
                    .setAudioAttributes(audioAttributes)
                    .setAudioFormat(
                        AudioFormat
                            .Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build(),
                    ).setTransferMode(AudioTrack.MODE_STATIC)
                    .setBufferSizeInBytes(samples.size * BYTES_PER_FLOAT)
                    .build()
            }.getOrNull() ?: return
        val written = track.write(samples, 0, samples.size, AudioTrack.WRITE_BLOCKING)
        if (written != samples.size) {
            track.release()
            return
        }
        currentTrack = track
        runCatching { track.play() }
    }

    private fun releaseCurrentTrack() {
        val track = currentTrack ?: return
        runCatching { track.stop() }
        track.release()
        currentTrack = null
    }
}
