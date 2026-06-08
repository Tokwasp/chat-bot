import matplotlib
matplotlib.use('Agg')
import matplotlib.font_manager as fm
import matplotlib.pyplot as plt
import matplotlib.patches as mpatches
from matplotlib.patches import FancyBboxPatch, FancyArrowPatch
import matplotlib.patheffects as pe

_NANUM = '/usr/share/fonts/truetype/nanum/NanumBarunGothic.ttf'
fm.fontManager.addfont(_NANUM)
_FONT = fm.FontProperties(fname=_NANUM).get_name()
matplotlib.rcParams['font.family'] = _FONT

# ── 공통 색상 ──────────────────────────────────────────────
C_CLIENT  = '#4A90D9'
C_SPRING  = '#6DB33F'
C_AWS     = '#FF9900'
C_DB      = '#7B68EE'
C_BG      = '#F8F9FA'
C_BORDER  = '#DEE2E6'
C_TEXT    = '#212529'
C_ARROW   = '#495057'
C_SSE     = '#E74C3C'

def box(ax, x, y, w, h, color, text, fontsize=11, text_color='white', radius=0.012):
    rect = FancyBboxPatch((x, y), w, h,
                          boxstyle=f'round,pad=0,rounding_size={radius}',
                          facecolor=color, edgecolor='white',
                          linewidth=1.5, zorder=3)
    ax.add_patch(rect)
    ax.text(x + w/2, y + h/2, text, ha='center', va='center',
            fontsize=fontsize, color=text_color, fontweight='bold',
            zorder=4, wrap=True)

def arrow(ax, x1, y1, x2, y2, label='', color=C_ARROW, lw=1.8, style='->', dashed=False):
    ls = '--' if dashed else '-'
    ax.annotate('', xy=(x2, y2), xytext=(x1, y1),
                arrowprops=dict(arrowstyle=style, color=color,
                                lw=lw, linestyle=ls),
                zorder=5)
    if label:
        mx, my = (x1+x2)/2, (y1+y2)/2
        ax.text(mx, my + 0.012, label, ha='center', va='bottom',
                fontsize=8, color=color, zorder=6,
                bbox=dict(facecolor='white', edgecolor='none', pad=1))

# ══════════════════════════════════════════════════════════
# 1. 아키텍처 다이어그램
# ══════════════════════════════════════════════════════════
fig, ax = plt.subplots(figsize=(8, 9))
ax.set_xlim(0, 1)
ax.set_ylim(0, 1)
ax.axis('off')
fig.patch.set_facecolor(C_BG)
ax.set_facecolor(C_BG)

ax.text(0.5, 0.97, 'AI 챗봇 — 시스템 아키텍처',
        ha='center', va='top', fontsize=15, fontweight='bold', color=C_TEXT)

BW, BH = 0.44, 0.10   # 박스 너비/높이
CX = 0.5               # 중앙 x

# ── 박스 4개 (위 → 아래) ───────────────────────────────────
boxes = [
    (0.80, C_CLIENT,  'Frontend Client'),
    (0.62, C_SPRING,  'Spring Boot'),
    (0.44, C_AWS,     'AWS Bedrock\n(Claude Sonnet / Haiku)'),
    (0.22, C_DB,      'H2 / MySQL'),
]
for by, bc, bt in boxes:
    box(ax, CX - BW/2, by, BW, BH, bc, bt, fontsize=12)

# ── 화살표 ─────────────────────────────────────────────────
# Client → Spring Boot (SSE + REST)
arrow(ax, CX - 0.04, 0.80, CX - 0.04, 0.72, 'POST /api/chat  (SSE)', color=C_SSE, lw=2)
arrow(ax, CX + 0.04, 0.80, CX + 0.04, 0.72, '/api/sessions/**  (REST)', color=C_CLIENT)
# Spring Boot → Bedrock
arrow(ax, CX, 0.62, CX, 0.54, 'ConverseStream API', color=C_AWS, lw=2)
# Spring Boot → DB
arrow(ax, CX + 0.22, 0.66, CX + 0.22, 0.32, 'JPA', color=C_DB, lw=1.8, dashed=True)

# ── 범례 ───────────────────────────────────────────────────
ax.plot([0.05, 0.12], [0.13, 0.13], color=C_SSE, lw=2.5)
ax.text(0.13, 0.13, ' SSE 스트리밍', va='center', fontsize=9, color=C_SSE)
ax.plot([0.05, 0.12], [0.08, 0.08], color=C_CLIENT, lw=2)
ax.text(0.13, 0.08, ' REST API', va='center', fontsize=9, color=C_CLIENT)
ax.plot([0.05, 0.12], [0.03, 0.03], color=C_DB, lw=2, linestyle='--')
ax.text(0.13, 0.03, ' DB 저장', va='center', fontsize=9, color=C_DB)

plt.tight_layout()
plt.savefig('/home/user/chat-bot/docs/architecture.png', dpi=150, bbox_inches='tight',
            facecolor=C_BG)
plt.close()
print('architecture.png saved')

# ══════════════════════════════════════════════════════════
# 2. SSE 시퀀스 다이어그램
# ══════════════════════════════════════════════════════════
fig, ax = plt.subplots(figsize=(12, 8))
ax.set_xlim(0, 1)
ax.set_ylim(0, 1)
ax.axis('off')
fig.patch.set_facecolor(C_BG)
ax.set_facecolor(C_BG)

ax.text(0.5, 0.98, 'SSE 스트리밍 시퀀스',
        ha='center', va='top', fontsize=15, fontweight='bold', color=C_TEXT)

# ── 참여자 박스 (상단) ─────────────────────────────────────
participants = [
    (0.08,  C_CLIENT, 'Client'),
    (0.32,  C_SPRING, 'ChatController'),
    (0.56,  C_SPRING, 'ChatService'),
    (0.80,  C_AWS,    'AWS Bedrock'),
]
BOX_W, BOX_H = 0.14, 0.07
TOP_Y = 0.82
LIFE_BOTTOM = 0.05

for px, pc, pt in participants:
    box(ax, px - BOX_W/2, TOP_Y, BOX_W, BOX_H, pc, pt, fontsize=10)
    # 생명선
    ax.plot([px, px], [TOP_Y, LIFE_BOTTOM],
            color='#ADB5BD', lw=1.2, linestyle='--', zorder=1)

def seq_arrow(ax, from_x, to_x, y, label, color=C_ARROW, dashed=False, ret=False):
    style = '<-' if ret else '->'
    ls = '--' if dashed else '-'
    ax.annotate('', xy=(to_x, y), xytext=(from_x, y),
                arrowprops=dict(arrowstyle=style, color=color, lw=1.8, linestyle=ls),
                zorder=5)
    lx = (from_x + to_x) / 2
    dy = 0.018
    ax.text(lx, y + dy, label, ha='center', va='bottom', fontsize=8.5,
            color=color, zorder=6,
            bbox=dict(facecolor='white', edgecolor='none', pad=1.5))

CX, CTRL_X, SVC_X, BEDR_X = 0.08, 0.32, 0.56, 0.80

steps = [
    # (from_x, to_x, y, label, color, dashed, ret)
    (CX,     CTRL_X, 0.72, 'POST /api/chat  {sessionId, message}',    C_CLIENT,  False, False),
    (CTRL_X, SVC_X,  0.64, 'stream(emitter, request)',                 C_SPRING,  False, False),
    (SVC_X,  BEDR_X, 0.56, 'converseStream(conversationRequest)',      C_AWS,     False, False),
    (BEDR_X, SVC_X,  0.48, 'text_delta: "안"',                         C_SSE,     True,  True),
    (SVC_X,  CX,     0.48, 'event: text  {"text":"안"}',               C_SSE,     True,  True),
    (BEDR_X, SVC_X,  0.40, 'text_delta: "녕하세요"',                    C_SSE,     True,  True),
    (SVC_X,  CX,     0.40, 'event: text  {"text":"녕하세요"}',          C_SSE,     True,  True),
    (BEDR_X, SVC_X,  0.32, 'message_stop  (end_turn)',                 C_AWS,     True,  True),
    (SVC_X,  CX,     0.32, 'event: done  {stopReason, usage}',         C_SPRING,  True,  True),
    (SVC_X,  SVC_X,  0.24, '응답 DB 저장',                              C_DB,      False, False),
]

# self-loop for DB save
for item in steps:
    from_x, to_x, y, label, color, dashed, ret = item
    if from_x == to_x:  # self message
        ax.annotate('', xy=(from_x + 0.08, y - 0.04), xytext=(from_x, y),
                    arrowprops=dict(arrowstyle='->', color=color, lw=1.5,
                                    connectionstyle='arc3,rad=-0.4'),
                    zorder=5)
        ax.text(from_x + 0.11, y - 0.02, label, ha='left', va='center',
                fontsize=8.5, color=color, zorder=6)
    else:
        seq_arrow(ax, from_x, to_x, y, label, color, dashed, ret)

# SSE 연결 유지 표시
ax.annotate('', xy=(CX, 0.15), xytext=(CX, 0.76),
            arrowprops=dict(arrowstyle='-', color=C_SSE, lw=2.5,
                            linestyle='-'),
            zorder=2)
ax.text(CX - 0.06, 0.45, 'SSE\n연결\n유지', ha='center', va='center',
        fontsize=7.5, color=C_SSE, rotation=90)

ax.plot([0.0, 0.1], [0.16, 0.16], color=C_SSE, lw=2); ax.text(0.12, 0.16, 'SSE 스트리밍', va='center', fontsize=8, color=C_SSE)
ax.plot([0.3, 0.4], [0.16, 0.16], color=C_SPRING, lw=2); ax.text(0.42, 0.16, '내부 호출', va='center', fontsize=8, color=C_SPRING)
ax.plot([0.55, 0.65], [0.16, 0.16], color=C_ARROW, lw=2, linestyle='--'); ax.text(0.67, 0.16, '반환/응답', va='center', fontsize=8, color=C_ARROW)
ax.plot([0.80, 0.90], [0.16, 0.16], color=C_DB, lw=2); ax.text(0.92, 0.16, 'DB 저장', va='center', fontsize=8, color=C_DB)

plt.tight_layout()
plt.savefig('/home/user/chat-bot/docs/sequence.png', dpi=150, bbox_inches='tight',
            facecolor=C_BG)
plt.close()
print('sequence.png saved')
