package vn.cavemind.compat;

import de.maxhenkel.voicechat.api.ForgeVoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.opus.OpusDecoder;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;
import vn.cavemind.CaveMind;
import vn.cavemind.sense.Ears;

@ForgeVoicechatPlugin
public final class Voice implements VoicechatPlugin {
    private static final Map<UUID, OpusDecoder> DEC = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LAST = new ConcurrentHashMap<>();
    private static final Map<UUID, Double> PEAK = new ConcurrentHashMap<>();

    @Override
    public String getPluginId() {
        return CaveMind.MODID;
    }

    @Override
    public void registerEvents(EventRegistration r) {
        r.registerEvent(MicrophonePacketEvent.class, this::mic);
        CaveMind.LOG.info("[CaveMind] Simple Voice Chat linked: talking is now a sound the things in the fog can hear");
    }

    private void mic(MicrophonePacketEvent e) {
        try {
            VoicechatConnection c = e.getSenderConnection();
            if (c == null || !(c.getPlayer().getPlayer() instanceof ServerPlayer p)) return;
            byte[] opus = e.getPacket().getOpusEncodedData();
            if (opus == null || opus.length == 0) return;
            UUID id = p.getUUID();
            OpusDecoder dec = DEC.computeIfAbsent(id, k -> e.getVoicechat().createDecoder());
            short[] pcm = dec.decode(opus);
            if (pcm == null || pcm.length == 0) return;
            double sum = 0;
            for (short s : pcm) sum += (double) s * s;
            double rms = Math.sqrt(sum / pcm.length) / 32768.0;
            double db = 20 * Math.log10(Math.max(1e-6, rms));
            PEAK.merge(id, db, Math::max);
            long now = System.currentTimeMillis();
            if (now - LAST.getOrDefault(id, 0L) < 450) return;
            double peak = PEAK.remove(id);
            if (peak < -42) return;
            LAST.put(id, now);
            boolean whisper = e.getPacket().isWhispering();
            double radius = peak > -18 ? 22 : peak > -28 ? 13 : 6;
            if (whisper) radius = Math.min(radius, 4);
            final double r = radius;
            p.server.execute(() -> {
                if (!p.isAlive() || p.isSpectator()) return;
                Ears.noise(p, p.position(), r, Ears.Kind.VOICE);
                Radio.spoke(p, r);
            });
        } catch (Throwable t) {
            CaveMind.LOG.debug("[CaveMind] voice level failed: {}", t.toString());
        }
    }

    public static void forget(UUID id) {
        OpusDecoder d = DEC.remove(id);
        if (d != null) d.close();
        LAST.remove(id);
        PEAK.remove(id);
    }
}
