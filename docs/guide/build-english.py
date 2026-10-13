from pathlib import Path
import re
import html

ROOT = Path(__file__).resolve().parent
translations = '''
Bet & Coin · 玩法指南|Bet & Coin · Player Guide
跳到操作指南|Skip to the guide
玩家指南 · 1.0.6|Player Guide · 1.0.6
PLAYER GUIDE / 玩法指南|PLAYER GUIDE
从第一枚筹码开始。|Start with your first chip.
选一张游戏桌，跟着步骤玩完第一局。详细规则，随时往下查。|Choose a game and follow the steps to play your first round. Find more rules below.
双人骰子|Two-player dice
二十一点|Twenty-one
颜色轮盘|Color roulette
老虎机|Slot machine
掷骰、留分，再决定冒不冒险。|Roll, score, and decide when to bank.
2 人 · 先到 10,000 分|2 players · First to 10,000
先约好下注，再开始。|Match your bets to begin.
两人站在相对的两侧，准备同一种筹码。|Stand on opposite sides with the same type of chip.
手持筹码右键桌面，每次投入 1 枚，每侧最多 5 枚。|Hold chips and right-click the table to bet one at a time, up to five per side.
双方面额与数量一致后，等待约 2 秒，游戏自动开始。|Once both bets match in value and quantity, the game starts after about two seconds.
随机决定先手，并自动掷出 6 颗骰子。切换空手，开始操作。|The starting player is chosen at random and six dice roll automatically. Switch to an empty hand to play.
计分器只显示已存入的总分。本回合暂存分，需要自己记住。|The counter shows banked points only. Keep track of your current turn score yourself.
01 / 选择|01 / SELECT
右键得分骰子|Right-click scoring dice
选中的骰子升起。再次右键可以取消；所选骰子必须全部能计分。|Selected dice rise. Right-click again to deselect. Every selected die must contribute to a scoring combination.
02 / 绿色|02 / GREEN
提交，暂存得分|Submit to add points
得分骰子移到白框。本回合分数增加，但还没存进计分器。|Scoring dice move to the white area. Their points join your turn score, but are not banked yet.
03 / 黄色|03 / YELLOW
重掷，继续冒险|Reroll to keep going
掷剩余骰子。先提交或取消当前选择；全部 6 颗已得分时可掷全套。|Roll the remaining dice after submitting or clearing your selection. If all six dice have scored, roll all six again.
04 / 红色|04 / RED
收分，交给对手|Bank and pass the turn
暂存分收入总分，结束回合。对手的第一掷自动进行。|Add your turn score to your banked total. Your opponent's first roll happens automatically.
没有得分骰子？本回合暂存分全部失去。|No scoring dice? You lose all points from this turn.
这就是 Farkle。已经存入计分器的总分保留，自动换对手行动。每次掷骰至少提交一次，才能重掷或收分。|That is a Farkle. Your banked total stays safe and play passes to your opponent. Submit scoring dice at least once after each roll before rerolling or banking.
先到 10,000 分，立即获胜|First to 10,000 wins immediately
按红色收分后判断胜负，不必刚好达到目标，也没有最后追分回合。|The red bank button checks for victory. You may exceed 10,000; there is no final catch-up turn.
双方下注，都归胜者|The winner takes both bets
胜方数字闪烁约 3 秒，随后筹码发到背包，溢出部分掉在胜者身边。|The winner's score flashes for about three seconds. Chips go into their inventory; any overflow drops beside them.
两人离开，再开下一局|Both players leave to reset
结果保留供查看。双方都离开桌旁后重置，不会自动续注。|The result stays on display until both players leave the table. There is no automatic repeat bet.
查看完整计分表|Full scoring table
骰子组合|Dice combination
分数|Points
单颗 1|Single 1
单颗 5|Single 5
三颗 1|Three 1s
三颗 2 / 3 / 4|Three 2s / 3s / 4s
三颗 5 / 6|Three 5s / 6s
四 / 五 / 六颗相同|Four / five / six of a kind
1～6 顺子|Straight: 1 through 6
三对不同点数|Three different pairs
四颗相同 + 一对|Four of a kind + a pair
两组三条|Two triplets
每次提交独立计分，系统取最高合法分数。先分开提交三个 5，不能事后合并成三条；不同次掷出的骰子也不能拼成组合。|Each submission scores separately, using the highest valid score. Three 5s submitted separately cannot later become a triplet. Dice from different rolls cannot form a new combination.
本模组三个 1 为 300 分。|In this mod, three 1s score 300 points.
离席、掉线或拆桌，会怎样？|Leaving, disconnecting, or breaking the table
未开局或开局闪烁时离席：退回自己的下注。|Leave before play begins or during the opening flash: your bet is refunded.
掷骰开始后离席、死亡、掉线或换维度：对手获胜。距桌中心超过 5 格视为离席，不保留重连席位。|After rolling begins, leaving, dying, disconnecting, or changing dimensions awards the win to your opponent. Moving more than five blocks from the center counts as leaving. Seats are not kept for reconnecting players.
同一次检查双方都离席，且胜者未确定：取消对局，各退各的筹码。|If both players are absent in the same check before a winner is decided, the game is canceled and both bets are refunded.
拆桌时胜者尚未确定：各退本金；已经确定胜者：双方筹码仍归胜者。|Breaking the table refunds both bets if no winner is decided. Otherwise, both bets still go to the winner.
领取者离线时，筹码掉在桌子附近。没有操作超时，仍在桌边但不操作会继续等待。|If the recipient is offline, chips drop near the table. There is no action timeout while a player remains at the table.
接近 21 点，别超过它。|Get close to 21. Don't go over.
1～3 人 · 对抗荷官|1–3 players · Against the dealer
选一个空栏位，投入一枚。|Pick a free seat and bet one chip.
站在桌子正面的左、中、右任一空栏位附近。|Stand near a free left, middle, or right position at the front of the table.
手持筹码右键该栏位，扣除 1 枚，自动开始。|Hold a chip and right-click that position. One chip is taken and play starts automatically.
开场动画期间加入可参加本局；之后加入预付下一局。|Join during the opening animation to play this round. Joining later prepays the next round.
观察纸牌，按左、中、右顺序行动；只在自己的回合做手势。|Watch the cards. Turns go left to right; use gestures only on your own turn.
每人独立与荷官比较。同一玩家只能占一个 Blackjack 栏位。|Each player competes with the dealer independently. You can occupy only one Blackjack seat.
01 / 发牌|01 / DEAL
先看两张起手牌|Check your two starting cards
玩家两张明牌；荷官一明一暗。A 可作 1 或 11，J、Q、K 为 10。|Your two cards are face up; the dealer has one up and one down. An ace is 1 or 11; J, Q, and K are 10.
02 / 要牌|02 / HIT
点头，再拿一张|Nod to take another card
等待发牌动画结束再操作。21 点自动停牌，超过 21 点爆牌。|Wait for the dealing animation to finish. At 21 you stand automatically; over 21 is a bust.
03 / 停牌|03 / STAND
摇头，结束行动|Shake your head to stand
轮到下一位玩家。所有人结束后，荷官翻开暗牌。|Play passes to the next player. Once everyone has finished, the dealer reveals the hidden card.
04 / 比较|04 / COMPARE
荷官补牌并结算|The dealer plays and pays
荷官低于 17 继续补牌，17 及以上停牌，包括软 17。|The dealer hits below 17 and stands on 17 or higher, including soft 17.
获胜 → 总返还 2 枚|Win → Receive 2 chips
自己没爆牌，点数高于荷官，或荷官爆牌。返还包含本金。|Without busting, beat the dealer's total or let the dealer bust. The return includes your original bet.
平局 → 退回 1 枚|Push → Get 1 chip back
自己没爆牌，与荷官点数相同。起手 21 没有特殊奖励。|Match the dealer without busting. A starting 21 has no special payout.
失败 → 不返还|Lose → No return
自己爆牌、点数较低或中途弃权，失去本局下注。|Bust, finish below the dealer, or forfeit mid-round to lose your bet.
清桌后会自动扣筹码，继续下一局。|After the table clears, another chip is taken for the next round.
从背包扣一枚与最初下注相同种类的筹码。没匹配筹码会离席。不想继续，派奖后及时离开自己的栏位附近。|One chip matching your original bet is taken from your inventory. With no matching chip, you leave the game. To stop playing, move away from your seat after the payout.
手势没反应？查看操作细节|Gesture not responding?
轮到自己时，试着把鼠标晃动幅度加大一些，连贯地做出点头或摇头动作。|On your turn, try moving the mouse farther with a smooth nodding or head-shaking motion.
距自己栏位中心超过 2 格或换维度视为离席，掉线立即释放已加载桌子的席位。开场期间、下一局等待期间及候场预付款可退款；进入准备发牌阶段后到实际派奖前离席，当前局按弃权处理。|Moving more than two blocks from your seat or changing dimensions counts as leaving. Disconnecting immediately frees seats at loaded tables. Opening-stage bets and next-round prepayments can be refunded; leaving after deal preparation begins and before the payout forfeits the current round.
拆桌时退回未结算下注和下一局预付款，已经结算的不再退款；领取者离线时退款掉在栏位附近。|Breaking the table refunds unsettled bets and next-round prepayments, but not completed payouts. Offline players' refunds drop near their seats.
押一种颜色，等轮盘停下。|Bet on a color and wait for the result.
1 人或多人 · 颜色下注|1+ players · Color bets
手持筹码，对准颜色区。|Hold chips and aim at a color.
从桌面上方右键红、黑或绿，每次下注 1 枚。|Right-click red, black, or green from above the table to bet one chip at a time.
第一笔下注开启 5 秒下注窗口，后续加注不延长时间。|The first bet opens a five-second betting window. Further bets do not extend it.
可以加注、押多个颜色，或分别使用两种筹码。|Add more chips, bet on multiple colors, or use both chip values separately.
窗口结束自动开奖，转动和结果等待阶段不能下注。|The wheel starts automatically when betting closes. You cannot bet while it spins or displays the result.
每个颜色区最多显示 5 枚，实际下注可以更多。显示筹码不等于下注上限。|Each color shows at most five chips, even if more have been bet. The display is not a betting limit.
01 / 下注|01 / BET
5 秒内选颜色|Choose within five seconds
多人可以参加同一轮，各人的颜色和筹码分别计算。|Multiple players can share a round. Each player's colors and chips are tracked separately.
02 / 转动|02 / SPIN
等待约 8 秒|Wait about eight seconds
轮盘自动转动并减速。此时下注已经锁定。|The wheel spins and slows automatically. Bets are now locked.
03 / 停下|03 / RESULT
看最终亮起的颜色|Watch the winning color
结果闪烁展示约 2 秒，然后支付押中的筹码。|The result flashes for about two seconds, then winning bets are paid.
04 / 再来|04 / AGAIN
下一笔注开启新局|The next bet starts a new round
结算后的桌面筹码只是记录，不能再次领取。|Chips left on the table after a payout are a record only. They cannot be collected again.
押中红色|Win on red
押中黑色|Win on black
押中绿色|Win on green
每下注 1 枚，拿回 |For each chip bet, receive 
2 枚|2 chips
10 枚|10 chips
拿回的筹码与下注面额相同，数量包含本金；没有押中则不返还。|Returns use the same chip value and include your original bet. Losing bets return nothing.
概率与下注规则|Odds and betting rules
68 格等概率：33 红、33 黑、2 绿。红黑各 33/68，绿为 2/68。仅支持颜色下注，没有数字、奇偶、大小下注。|All 68 segments are equally likely: 33 red, 33 black, and 2 green. Red and black each have a 33/68 chance; green has 2/68. Only color bets are supported, with no number, odd/even, or high/low bets.
同一玩家同一颜色同种筹码最多累计 4,096 枚，全桌最多 256 条下注记录。结算后所有下注者离开 6 格范围时自动清桌。|Each player can bet up to 4,096 chips of one type on a color. The table holds up to 256 betting entries. After payout, it clears once all bettors leave the six-block range.
下注窗口内离席、死亡、掉线或换维度会取消下注并退还，每秒检查一次；距桌中心超过 6 格为离席。开始转动后离开不会取消，照常按结果结算。|During betting, leaving, dying, disconnecting, or changing dimensions cancels and refunds your bets. Checks run once a second; more than six blocks from the center counts as leaving. Once spinning starts, your bets remain active and settle normally.
离线奖励或退款暂存在该桌，桌子加载时会在玩家上线后发放。下注阶段拆桌退本金；转动后拆桌按已选定的颜色结算。拆桌时尚未领取的离线筹码掉在桌子附近。|Offline payouts and refunds stay at the table and are delivered when the player returns online while the table is loaded. Breaking it during betting refunds bets; after spinning starts, it pays according to the chosen result. Undelivered offline chips drop near the broken table.
三个相同图案，就是中奖。|Match all three symbols to win.
单次启动 · 手动续局|One spin · Restart manually
手持一枚筹码，右键启动。|Hold a chip and right-click to start.
5 或 20 筹码都可使用，点击上半部或下半部即可。|Use a 5 or 20 chip. Right-click either half of the machine.
左、中、右转子依次停止，总过程约 3.5 秒。|The left, middle, and right reels stop in order, taking about 3.5 seconds in total.
三个相同图案，灯牌亮起，吐出两枚同面额筹码。|If all three symbols match, the sign lights up and two chips of the same value drop out.
旋转中再次右键不会重复扣注。结束后再右键开始下一次。|Right-clicking during a spin does not take another bet. Right-click after it finishes to spin again.
四种图案，奖励相同|Four symbols, one payout
苹果、金锭、绿宝石、钻石；三个同图案均返还两枚筹码，不发图案对应物品。|Apple, gold ingot, emerald, or diamond: any matching trio returns two chips, not the pictured item.
总返还 2 枚，净赢 1 枚|2 chips returned, 1 chip profit
各转子独立等概率，中奖概率 1/16。图案不同没有返还。|Each reel is independent and equally likely. The chance to win is 1/16. Nonmatching symbols return nothing.
到机器正面拾取|Pick up chips in front
奖励直接掉落，可由附近玩家拾取，不自动存入背包。|Rewards drop as items for nearby players to pick up. They do not go straight into your inventory.
离开或拆掉机器，会怎样？|Leaving or breaking the machine
启动后不记录玩家席位，离开或掉线不取消旋转。旋转中拆掉任一半部，按已经抽出的结果立即结算：中奖吐出两枚，没中奖不退款。|There are no player seats. Leaving or disconnecting does not cancel a spin. Breaking either half during a spin immediately settles the already chosen result: two chips for a win, no refund for a loss.
中文指南初版 · Minecraft 1.16.5 / Forge · |Player Guide · Minecraft 1.16.5 / Forge · 
返回当前指南 ↑|Back to the guide ↑
'''
mapping = dict(line.split('|', 1) for line in translations.strip('\n').splitlines())
source = (ROOT/'zh.html').read_text(encoding='utf-8')

switch_css = '''
.header-tools{display:flex;align-items:center;gap:18px}
.language-switch{display:flex;gap:3px;border:1px solid #53676b;padding:3px;border-radius:4px;white-space:nowrap}
.language-switch a{font-size:12px;text-decoration:none;color:#c3d9d9;padding:3px 8px;border-radius:2px}
.language-switch a[aria-current=page]{background:#e9bd47;color:#162b31;font-weight:750}
.game-head{flex-wrap:wrap}.tables{overflow-x:auto}
@media(max-width:600px){.header-tools{gap:7px}.header-tools .edition{display:none}.language-switch a{padding:3px 6px;font-size:11px}}
@media print{.language-switch{display:none}}
'''
if 'class="language-switch"' not in source:
 source = source.replace('<span class="edition">玩家指南 · 1.0.6</span>', '<div class="header-tools"><span class="edition">玩家指南 · 1.0.6</span><nav class="language-switch" aria-label="Language"><a href="zh.html" lang="zh-CN" aria-current="page">中文</a><a href="index.html" lang="en">English</a></nav></div>')
 source = source.replace('</style>',switch_css+'</style>')
 source = source.replace('select(location.hash.slice(1));', '''select(location.hash.slice(1));
document.querySelectorAll('.language-switch a').forEach(link => {
  link.addEventListener('click', () => {
    const active = panels.find(panel => !panel.hidden);
    link.hash = active ? active.id : 'farkle';
  });
});''')
 (ROOT/'zh.html').write_text(source,encoding='utf-8')

missing=[]
def translate(match):
 text=match.group(1)
 if not re.search('[\u4e00-\u9fff]',text): return match.group(0)
 if text=='中文': return match.group(0)
 if text not in mapping:
  missing.append(text); return match.group(0)
 return '>'+html.escape(mapping[text],quote=False)+'<'
english=re.sub(r'>([^<>]+)<',translate,source)
if missing: raise ValueError('Missing translations: '+repr(missing))
english=english.replace('lang="zh-CN">','lang="en">',1)
english=english.replace('href="zh.html" lang="zh-CN" aria-current="page"','href="zh.html" lang="zh-CN"')
english=english.replace('href="index.html" lang="en"','href="index.html" lang="en" aria-current="page"')
attrs={
'Bet & Coin 图解玩法指南：Farkle、Blackjack、轮盘与老虎机。':'Bet & Coin illustrated player guide: Farkle, Blackjack, Roulette, and Slot Machine.',
'选择游戏':'Choose a game', '骰子、纸牌与筹码':'Dice, cards, and chips', '轮盘中奖返还':'Roulette payouts',
'Farkle 游戏桌：下注筹码、右键选骰，以及绿色提交、黄色重掷、红色收分按钮。':'Farkle: bet chips, right-click dice, use green to submit, yellow to reroll, and red to bank.',
'二十一点：手持筹码右键下注，上下移动鼠标点头要牌，左右移动鼠标摇头停牌。':'Blackjack: hold a chip and right-click to bet. Move the mouse vertically to nod and hit, horizontally to shake your head and stand.',
'轮盘桌：手持筹码右键下注区，轮盘区闪烁显示开奖结果。':'Roulette: hold a chip and right-click the betting area. The wheel flashes the winning color.',
'老虎机：手持筹码，右键拉杆。':'Slot machine: hold a chip and right-click the lever.'}
for cn,en in attrs.items(): english=english.replace(cn,html.escape(en,quote=True))
for game,cn in [('farkle','farkle-annotated-v4-unified.png'),('blackjack','blackjack-annotated-v4-unified.png'),('roulette','roulette-annotated-v5-front.png'),('slot','slot-annotated-v4-unified.png')]:
 english=english.replace(cn,f'{game}-annotated-en-v1.png')
(ROOT/'index.html').write_text(english,encoding='utf-8')
(ROOT/'en.html').write_text(english,encoding='utf-8')
print('Created English guide and bilingual navigation.')
