"""Sintetiza os efeitos sonoros provisórios do Assemble (WAV mono 16 bits, 44,1 kHz). Sem licença: tudo é gerado aqui."""
import wave

import numpy as np

SR = 44_100
OUT = 'app/src/main/res/raw'  # rode a partir da raiz do repositório
rng = np.random.default_rng(7)


def t_axis(seconds):
    return np.arange(int(SR * seconds)) / SR


def env(n, attack=0.004, decay=6.0):
    """Ataque rápido e queda exponencial."""
    x = np.arange(n) / SR
    a = np.minimum(1.0, x / attack)
    return a * np.exp(-decay * x)


def tone(freq, seconds, decay=6.0, harmonics=((1, 1.0),)):
    t = t_axis(seconds)
    wave_ = sum(amp * np.sin(2 * np.pi * freq * k * t) for k, amp in harmonics)
    return wave_ * env(len(t), decay=decay)


def bell(freq, seconds, decay=5.0):
    # Parciais inarmônicas leves: soa como sino, não como bip.
    return tone(freq, seconds, decay, harmonics=((1, 1.0), (2.01, 0.45), (3.97, 0.2), (5.4, 0.08)))


def lowpass_sweep(noise, start_hz, end_hz):
    """Passa-baixa de um polo com corte deslizante: o 'whoosh'."""
    n = len(noise)
    cutoffs = np.geomspace(start_hz, end_hz, n)
    alpha = 1 - np.exp(-2 * np.pi * cutoffs / SR)
    out = np.zeros(n)
    y = 0.0
    for i in range(n):
        y += alpha[i] * (noise[i] - y)
        out[i] = y
    return out


def swish(seconds, start_hz, end_hz, peak=0.5):
    n = int(SR * seconds)
    noise = lowpass_sweep(rng.standard_normal(n), start_hz, end_hz)
    shape = np.sin(np.linspace(0, np.pi, n)) ** 2
    noise = noise / np.max(np.abs(noise))
    return noise * shape * peak


def place(buffer, sound, at_seconds, gain=1.0):
    start = int(SR * at_seconds)
    end = min(len(buffer), start + len(sound))
    buffer[start:end] += sound[: end - start] * gain


def fade_out(sound, seconds=0.01):
    n = min(len(sound), int(SR * seconds))
    sound[-n:] *= np.linspace(1, 0, n)
    return sound


def save(name, data, peak=0.8):
    data = data / max(1e-9, np.max(np.abs(data))) * peak
    pcm = (data * 32767).astype('<i2')
    with wave.open(f'{OUT}/{name}.wav', 'wb') as f:
        f.setnchannels(1)
        f.setsampwidth(2)
        f.setframerate(SR)
        f.writeframes(pcm.tobytes())
    print(name, f'{len(data) / SR:.2f}s', f'{pcm.nbytes // 1024} KB')


# Pass: ar passando, grave e descendo.
save('sfx_pass', fade_out(swish(0.22, 2400, 500)), peak=0.45)

# Assemble: o mesmo gesto, mais agudo e subindo, com um estalo no começo.
assemble = swish(0.26, 700, 5200)
place(assemble, tone(1400, 0.05, decay=60), 0.0, 0.5)
save('sfx_assemble', fade_out(assemble), peak=0.55)

# Match: impacto grave + brilho de sinos subindo (C5 E5 G5 C6) + cauda.
match = np.zeros(int(SR * 1.8))
place(match, tone(78, 0.5, decay=9, harmonics=((1, 1.0), (2, 0.4))), 0.0, 1.0)
place(match, swish(0.35, 6000, 800, 1.0) * env(int(SR * 0.35), decay=8), 0.0, 0.5)
for i, freq in enumerate((523.25, 659.25, 783.99, 1046.5)):
    place(match, bell(freq, 1.3, decay=3.6), 0.06 + i * 0.075, 0.5)
place(match, bell(2093.0, 1.0, decay=4.5), 0.38, 0.18)
save('sfx_match', fade_out(match, 0.05), peak=0.9)

# Conquista: dois sinos curtos (E6, A6).
ach = np.zeros(int(SR * 0.8))
place(ach, bell(1318.5, 0.6, decay=6.5), 0.0, 0.7)
place(ach, bell(1760.0, 0.7, decay=5.5), 0.11, 0.8)
save('sfx_achievement', fade_out(ach, 0.05), peak=0.7)

# Mensagem recebida: 'pop' suave, subindo um pouco.
n = int(SR * 0.12)
freq = np.linspace(520, 760, n)
phase = 2 * np.pi * np.cumsum(freq) / SR
save('sfx_message_in', fade_out(np.sin(phase) * env(n, attack=0.006, decay=28)), peak=0.5)

# Mensagem enviada: tique curto e seco.
n = int(SR * 0.06)
freq = np.linspace(1500, 1100, n)
phase = 2 * np.pi * np.cumsum(freq) / SR
save('sfx_message_out', fade_out(np.sin(phase) * env(n, attack=0.002, decay=70)), peak=0.4)

# Splash: sopro subindo e um sino no fim, quando a logo encaixa.
splash = np.zeros(int(SR * 1.3))
place(splash, swish(0.7, 400, 4200, 1.0), 0.0, 0.5)
place(splash, bell(880.0, 0.7, decay=4.5), 0.62, 0.6)
place(splash, bell(1318.5, 0.6, decay=5.5), 0.66, 0.3)
save('sfx_splash', fade_out(splash, 0.05), peak=0.6)
