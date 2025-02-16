/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307, USA.
 *
 * http://www.gnu.org/copyleft/gpl.html
 */
package net.l1j.server.model.skill;

import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.lang.String;

import javolution.util.FastTable;

import net.l1j.Config;
import net.l1j.server.ActionCodes;
import net.l1j.server.datatables.NpcTable;
import net.l1j.server.datatables.PolyTable;
import net.l1j.server.datatables.SkillsTable;
import net.l1j.server.model.L1Awake;
import net.l1j.server.model.L1CastleLocation;
import net.l1j.server.model.L1Character;
import net.l1j.server.model.L1CurseParalysis;
import net.l1j.server.model.L1EffectSpawn;
import net.l1j.server.model.L1Location;
import net.l1j.server.model.L1Magic;
import net.l1j.server.model.L1Object;
import net.l1j.server.model.L1PcInventory;
import net.l1j.server.model.L1PinkName;
import net.l1j.server.model.L1PolyMorph;
import net.l1j.server.model.L1Teleport;
import net.l1j.server.model.L1War;
import net.l1j.server.model.L1World;
import net.l1j.server.model.id.SystemMessageId;
import net.l1j.server.model.instance.*;
import net.l1j.server.model.poison.L1DamagePoison;
import net.l1j.server.model.trap.L1WorldTraps;
import net.l1j.server.serverpackets.S_ChangeHeading;
import net.l1j.server.serverpackets.S_ChangeName;
import net.l1j.server.serverpackets.S_ChangeShape;
import net.l1j.server.serverpackets.S_CharVisualUpdate;
import net.l1j.server.serverpackets.S_ChatPacket;
import net.l1j.server.serverpackets.S_CurseBlind;
import net.l1j.server.serverpackets.S_RemoveObject;
import net.l1j.server.serverpackets.S_DexUp;
import net.l1j.server.serverpackets.S_DoActionGFX;
import net.l1j.server.serverpackets.S_DoActionShop;
import net.l1j.server.serverpackets.S_HPUpdate;
import net.l1j.server.serverpackets.S_Invis;
import net.l1j.server.serverpackets.S_MPUpdate;
import net.l1j.server.serverpackets.S_Message_YN;
import net.l1j.server.serverpackets.S_NpcChatPacket;
import net.l1j.server.serverpackets.S_OwnCharAttrDef;
import net.l1j.server.serverpackets.S_OwnCharStatus;
import net.l1j.server.serverpackets.S_Paralysis;
import net.l1j.server.serverpackets.S_Poison;
import net.l1j.server.serverpackets.S_RangeSkill;
import net.l1j.server.serverpackets.S_SPMR;
import net.l1j.server.serverpackets.S_ServerMessage;
import net.l1j.server.serverpackets.S_ShowPolyList;
import net.l1j.server.serverpackets.S_ShowSummonList;
import net.l1j.server.serverpackets.S_SkillBrave;
import net.l1j.server.serverpackets.S_SkillHaste;
import net.l1j.server.serverpackets.S_SkillIconAura;
import net.l1j.server.serverpackets.S_SkillIconGFX;
import net.l1j.server.serverpackets.S_SkillIconShield;
import net.l1j.server.serverpackets.S_SkillIconWindShackle;
import net.l1j.server.serverpackets.S_SkillSound;
import net.l1j.server.serverpackets.S_Sound;
import net.l1j.server.serverpackets.S_StrUp;
import net.l1j.server.serverpackets.S_TrueTarget;
import net.l1j.server.serverpackets.S_UseAttackSkill;
import net.l1j.server.templates.L1BookMark;
import net.l1j.server.templates.L1Npc;
import net.l1j.server.templates.L1Skills;
import net.l1j.server.types.Base;
import net.l1j.util.RandomArrayList;

import static net.l1j.server.model.skill.SkillId.*;

public class SkillUse {
	private final static Logger _log = Logger.getLogger(SkillUse.class.getName());

	private L1Skills _skill;
	private int _skillId;
	private int _getBuffDuration;
	private int _shockStunDuration;
	private int _getBuffIconDuration;
	private int _targetID;
	private int _mpConsume = 0;
	private int _hpConsume = 0;
	private int _targetX = 0;
	private int _targetY = 0;
	private String _message = null;
	private int _skillTime = 0;
	private boolean _isPK = false;
	private int _bookmarkId = 0;
	private int _itemobjid = 0;
	private boolean _checkedUseSkill = false; // 事前チェック濟みか
	private int _leverage = 10; // 1/10倍なので10で1倍
	private boolean _isFreeze = false;
	private boolean _isCounterMagic = true;
	private boolean _isGlanceCheckFail = false;

	private L1Character _user = null;
	private L1Character _target = null;

	private L1PcInstance _player = null;
	private L1NpcInstance _npc = null;
	private L1NpcInstance _targetNpc = null;

	private int _skillType = 0;
	private final static int NORMAL = Base.SKILL_TYPE[0];
	private final static int LOGIN = Base.SKILL_TYPE[1];
	private final static int SPELLSC = Base.SKILL_TYPE[2];
	private final static int NPCBUFF = Base.SKILL_TYPE[3];
	private final static int GMBUFF = Base.SKILL_TYPE[4];

	private int _targetType;
	private final static int PC_PC = Base.TARGET_TYPE[1];
	private final static int PC_NPC = Base.TARGET_TYPE[2];
	private final static int NPC_PC = Base.TARGET_TYPE[3];
	private final static int NPC_NPC = Base.TARGET_TYPE[4];

	private FastTable<TargetStatus> _targetList;

	private static final int[] CAST_WITH_INVIS = { 1, 2, 3, 5, 8, 9, 12, 13, 14, 19, 21, 26, 31,
			32, 35, 37, 42, 43, 44, 48, 49, 52, 54, 55, 57, 60, 61, 63, 67, 68, 69, 72, 73, 75, 78,
			79, SKILL_REDUCTION_ARMOR, SKILL_BOUNCE_ATTACK, SKILL_SOLID_CARRIAGE,
			SKILL_COUNTER_BARRIER, 97, 98, 99, 100, 101, 102, 104, 105, 106, 107, 109, 110, 111,
			113, 114, 115, 116, 117, 118, 129, 130, 131, 133, 134, 137, 138, 146, 147, 148, 149,
			150, 151, 155, 156, 158, 159, 163, 164, 165, 166, 168, 169, 170, 171,
			SKILL_SOUL_OF_FLAME, SKILL_ADDITIONAL_FIRE, SKILL_DRAGON_SKIN, SKILL_AWAKEN_ANTHARAS,
			SKILL_AWAKEN_FAFURION, SKILL_AWAKEN_VALAKAS, SKILL_MIRROR_IMAGE, SKILL_ILLUSION_OGRE,
			SKILL_ILLUSION_LICH, SKILL_PATIENCE, SKILL_ILLUSION_DIA_GOLEM, SKILL_INSIGHT,
			SKILL_ILLUSION_AVATAR };

	private static final int[] EXCEPT_COUNTER_MAGIC = { 1, 2, 3, 5, 8, 9, 12, 13, 14, 19, 21, 26,
			31, 32, 35, 37, 42, 43, 44, 48, 49, 52, 54, 55, 57, 60, 61, 63, 67, 68, 69, 72, 73, 75,
			78, 79, SKILL_STUN_SHOCK, SKILL_REDUCTION_ARMOR, SKILL_BOUNCE_ATTACK,
			SKILL_SOLID_CARRIAGE, SKILL_COUNTER_BARRIER, 97, 98, 99, 100, 101, 102, 104, 105, 106,
			107, 109, 110, 111, 113, 114, 115, 116, 117, 118, 129, 130, 131, 132, 134, 137, 138,
			146, 147, 148, 149, 150, 151, 155, 156, 158, 159, 161, 163, 164, 165, 166, 168, 169,
			170, 171, SKILL_SOUL_OF_FLAME, SKILL_ADDITIONAL_FIRE, SKILL_DRAGON_SKIN,
			SKILL_FOE_SLAYER, SKILL_AWAKEN_ANTHARAS, SKILL_AWAKEN_FAFURION, SKILL_AWAKEN_VALAKAS,
			SKILL_MIRROR_IMAGE, SKILL_ILLUSION_OGRE, SKILL_ILLUSION_LICH, SKILL_PATIENCE, 10026,
			10027, SKILL_ILLUSION_DIA_GOLEM, SKILL_INSIGHT, SKILL_ILLUSION_AVATAR, 10028, 10029 };

	public SkillUse() {
	}

	private static class TargetStatus {
		private L1Character _target = null;
		private boolean _isAction = false; // ダメージモーションが發生するか？
		private boolean _isSendStatus = false; // キャラクターステータスを送信するか？（ヒール、スローなど狀態が變わるとき送る）
		private boolean _isCalc = true; // ダメージや確率魔法の計算をする必要があるか？

		public TargetStatus(L1Character _cha) {
			_target = _cha;
		}

		public TargetStatus(L1Character _cha, boolean _flg) {
			_isCalc = _flg;
		}

		public L1Character getTarget() {
			return _target;
		}

		public boolean isCalc() {
			return _isCalc;
		}

		public void isAction(boolean _flg) {
			_isAction = _flg;
		}

		public boolean isAction() {
			return _isAction;
		}

		public void isSendStatus(boolean _flg) {
			_isSendStatus = _flg;
		}

		public boolean isSendStatus() {
			return _isSendStatus;
		}
	}

	/*
	 * 1/10倍で表現する。
	 */
	public void setLeverage(int i) {
		_leverage = i;
	}

	public int getLeverage() {
		return _leverage;
	}

	private boolean isCheckedUseSkill() {
		return _checkedUseSkill;
	}

	private void setCheckedUseSkill(boolean flg) {
		_checkedUseSkill = flg;
	}

	public boolean checkUseSkill(L1PcInstance player, int skillid, int target_id, int x, int y, String message, int time, int type, L1Character attacker) {
		// 初期設定ここから
		setCheckedUseSkill(true);
		_targetList = new FastTable<TargetStatus>(); // ターゲットリストの初期化

		_skill = SkillsTable.getInstance().getTemplate(skillid);
		_skillId = skillid;
		_targetX = x;
		_targetY = y;
		_message = message;
		_skillTime = time;
		_skillType = type;
		boolean checkedResult = true;

		if (attacker == null) {
			// pc
			_player = player;
			_user = _player;
		} else {
			// npc
			_npc = (L1NpcInstance) attacker;
			_user = _npc;
		}

		if (_skill.getTarget().equals("none")) {
			_targetID = _user.getId();
			_targetX = _user.getX();
			_targetY = _user.getY();
		} else {
			_targetID = target_id;
		}

		if (type == NORMAL) { // 通常の魔法使用時
			checkedResult = isNormalSkillUsable();
		} else if (type == SPELLSC) { // スペルスクロール使用時
			checkedResult = isSpellScrollUsable();
		} else if (type == NPCBUFF) {
			checkedResult = true;
		}
		if (!checkedResult) {
			return false;
		}

		// ファイアーウォール、ライフストリームは詠唱對象が座標
		// キューブは詠唱者の座標に配置されるため例外
		if (_skillId == SKILL_FIRE_WALL || _skillId == SKILL_LIFE_STREAM) {
			return true;
		}

		L1Object l1object = L1World.getInstance().findObject(_targetID);
		if (l1object instanceof L1ItemInstance) {
			_log.fine("skill target item name: " + ((L1ItemInstance) l1object).getViewName());
			// スキルターゲットが精靈の石になることがある。
			// Linux環境で確認（Windowsでは未確認）
			// 2008.5.4追記：地面のアイテムに魔法を使うとなる。繼續してもエラーになるだけなのでreturn
			return false;
		}
		if (_user instanceof L1PcInstance) {
			if (l1object instanceof L1PcInstance) {
				_targetType = PC_PC;
			} else {
				_targetType = PC_NPC;
				_targetNpc = (L1NpcInstance) l1object;
			}
		} else if (_user instanceof L1NpcInstance) {
			if (l1object instanceof L1PcInstance) {
				_targetType = NPC_PC;
			} else if (_skill.getTarget().equals("none")) {
				_targetType = NPC_PC;
			} else {
				_targetType = NPC_NPC;
				_targetNpc = (L1NpcInstance) l1object;
			}
		}

		// テレポート、マステレポートは對象がブックマークID
		if (_skillId == SKILL_TELEPORT || _skillId == SKILL_MASS_TELEPORT) {
			_bookmarkId = target_id;
		}
		// 對象がアイテムのスキル
		if (_skillId == SKILL_CREATE_MAGICAL_WEAPON || _skillId == SKILL_PURIFY_STONE
				|| _skillId == SKILL_BLESSED_ARMOR || _skillId == SKILL_ENCHANT_WEAPON
				|| _skillId == SKILL_SHADOW_FANG) {
			_itemobjid = target_id;
		}
		_target = (L1Character) l1object;

		if (!(_target instanceof L1MonsterInstance) && _skill.getTarget().equals("attack") && _user.getId() != target_id) {
			_isPK = true; // ターゲットがモンスター以外で攻擊系スキルで、自分以外の場合PKモードとする。
		}

		// 初期設定ここまで

		// 事前チェック
		if (!(l1object instanceof L1Character)) { // ターゲットがキャラクター以外の場合何もしない。
			checkedResult = false;
		}
		makeTargetList(); // ターゲットの一覽を作成
		if (_targetList.size() == 0 && (_user instanceof L1NpcInstance)) {
			checkedResult = false;
		}
		// 事前チェックここまで
		return checkedResult;
	}

	/**
	 * 通常のスキル使用時に使用者の狀態からスキルが使用可能であるか判斷する
	 * 
	 * @return false スキルが使用不可能な狀態である場合
	 */
	private boolean isNormalSkillUsable() {
		// スキル使用者がPCの場合のチェック
		if (_user instanceof L1PcInstance) {
			L1PcInstance pc = (L1PcInstance) _user;

			if (pc.isParalyzed()) { // 麻痺‧凍結狀態か
				return false;
			}
			if ((pc.isInvisble() || pc.isInvisDelay()) && !isInvisUsableSkill()) { // インビジ中に使用不可のスキル
				return false;
			}
			if (pc.getInventory().getWeight240() >= 197) { // 重量オーバーならスキルを使用できない
				pc.sendPackets(new S_ServerMessage(SystemMessageId.$316));
				return false;
			}
			int polyId = pc.getTempCharGfx();
			L1PolyMorph poly = PolyTable.getInstance().getTemplate(polyId);
			// 魔法が使えない變身
			if (poly != null && !poly.canUseSkill()) {
				pc.sendPackets(new S_ServerMessage(SystemMessageId.$285));
				return false;
			}

			if (!isAttrAgrees()) { // 精靈魔法で、屬性が一致しなければ何もしない。
				return false;
			}

			if (_skillId == SKILL_PROTECTION_FROM_ELEMENTAL && pc.getElfAttr() == 0) {
				pc.sendPackets(new S_ServerMessage(SystemMessageId.$280));
				return false;
			}

			// スキルディレイ中使用不可
			if (pc.isSkillDelay()) {
				return false;
			}

			// サイレンス狀態では使用不可
			if (pc.hasSkillEffect(SKILL_SILENCE)
					|| pc.hasSkillEffect(SKILL_AREA_OF_SILENCE)
					|| pc.hasSkillEffect(STATUS_POISON_SILENCE)) {
				pc.sendPackets(new S_ServerMessage(SystemMessageId.$285));
				return false;
			}

			// DIGはロウフルでのみ使用可
			if (_skillId == SKILL_DESTROY && pc.getLawful() < 500) {
				// このメッセージであってるか未確認
				pc.sendPackets(new S_ServerMessage(SystemMessageId.$352, "$967"));
				return false;
			}

			// 同じキューブは效果範圍外であれば配置可能
			if (_skillId == SKILL_CUBE_IGNITION || _skillId == SKILL_CUBE_QUAKE
					|| _skillId == SKILL_CUBE_SHOCK || _skillId == SKILL_CUBE_BALANCE) {
				boolean isNearSameCube = false;
				int gfxId = 0;
				for (L1Object obj : L1World.getInstance()
						.getVisibleObjects(pc, 3)) {
					if (obj instanceof L1EffectInstance) {
						L1EffectInstance effect = (L1EffectInstance) obj;
						gfxId = effect.getGfxId();
						if (_skillId == SKILL_CUBE_IGNITION && gfxId == 6706
								|| _skillId == SKILL_CUBE_QUAKE && gfxId == 6712
								|| _skillId == SKILL_CUBE_SHOCK && gfxId == 6718
								|| _skillId == SKILL_CUBE_BALANCE && gfxId == 6724) {
							isNearSameCube = true;
							break;
						}
					}
				}
				if (isNearSameCube) {
					pc.sendPackets(new S_ServerMessage(SystemMessageId.$1412));
					return false;
				}
			}

			// 覺醒狀態では覺醒スキル以外使用不可
			if (pc.getAwakeSkillId() == SKILL_AWAKEN_ANTHARAS
					&& _skillId != SKILL_AWAKEN_ANTHARAS && _skillId != SKILL_MAGMA_BREATH
					&& _skillId != SKILL_SHOCK_SKIN && _skillId != SKILL_FREEZING_BREATH
					|| pc.getAwakeSkillId() == SKILL_AWAKEN_FAFURION
					&& _skillId != SKILL_AWAKEN_FAFURION && _skillId != SKILL_MAGMA_BREATH
					&& _skillId != SKILL_SHOCK_SKIN && _skillId != SKILL_FREEZING_BREATH
					|| pc.getAwakeSkillId() == SKILL_AWAKEN_VALAKAS
					&& _skillId != SKILL_AWAKEN_VALAKAS && _skillId != SKILL_MAGMA_BREATH
					&& _skillId != SKILL_SHOCK_SKIN && _skillId != SKILL_FREEZING_BREATH) {
				pc.sendPackets(new S_ServerMessage(SystemMessageId.$1385));
				return false;
			}

			if (isItemConsume() == false && !_player.isGm()) { // 消費アイテムはあるか
				_player.sendPackets(new S_ServerMessage(SystemMessageId.$299));
				return false;
			}
		}
		// スキル使用者がNPCの場合のチェック
		else if (_user instanceof L1NpcInstance) {

			// サイレンス狀態では使用不可
			if (_user.hasSkillEffect(SKILL_SILENCE)) {
				// NPCにサイレンスが掛かっている場合は1回だけ使用をキャンセルさせる效果。
				_user.removeSkillEffect(SKILL_SILENCE);
				return false;
			}
		}

		// PC、NPC共通のチェック
		if (!isHPMPConsume()) { // 消費HP、MPはあるか
			return false;
		}
		return true;
	}

	/**
	 * スペルスクロール使用時に使用者の狀態からスキルが使用可能であるか判斷する
	 * 
	 * @return false スキルが使用不可能な狀態である場合
	 */
	private boolean isSpellScrollUsable() {
		// スペルスクロールを使用するのはPCのみ
		L1PcInstance pc = (L1PcInstance) _user;

		if (pc.isParalyzed()) { // 麻痺‧凍結狀態か
			return false;
		}

		// インビジ中に使用不可のスキル
		if ((pc.isInvisble() || pc.isInvisDelay()) && !isInvisUsableSkill()) {
			return false;
		}

		return true;
	}

	// インビジ中に使用可能なスキルかを返す
	private boolean isInvisUsableSkill() {
		for (int skillId : CAST_WITH_INVIS) {
			if (skillId == _skillId) {
				return true;
			}
		}
		return false;
	}

	public void handleCommands(L1PcInstance player, int skillId, int targetId, int x, int y, String message, int timeSecs, int type) {
		L1Character attacker = null;
		handleCommands(player, skillId, targetId, x, y, message, timeSecs, type, attacker);
	}

	public void handleCommands(L1PcInstance player, int skillId, int targetId, int x, int y, String message, int timeSecs, int type, L1Character attacker) {

		try {
			// 事前チェックをしているか？
			if (!isCheckedUseSkill()) {
				boolean isUseSkill = checkUseSkill(player, skillId, targetId, x, y, message, timeSecs, type, attacker);

				if (!isUseSkill) {
					failSkill();
					return;
				}
			}

			if (type == NORMAL) { // 魔法詠唱時
				if (!_isGlanceCheckFail || _skill.getArea() > 0 || _skill.getTarget().equals("none")) {
					runSkill();
					useConsume();
					sendGrfx(true);
					sendFailMessageHandle();
					setDelay();
				}
			} else if (type == LOGIN) { // ログイン時（HPMP材料消費なし、グラフィックなし）
				runSkill();
			} else if (type == SPELLSC) { // スペルスクロール使用時（HPMP材料消費なし）
				runSkill();
				sendGrfx(true);
			} else if (type == GMBUFF) { // GMBUFF使用時（HPMP材料消費なし、魔法モーションなし）
				runSkill();
				sendGrfx(false);
			} else if (type == NPCBUFF) { // NPCBUFF使用時（HPMP材料消費なし）
				runSkill();
				sendGrfx(true);
			}
			setCheckedUseSkill(false);
		} catch (Exception e) {
			_log.log(Level.SEVERE, "", e);
		}
	}

	/**
	 * スキルの失敗處理(PCのみ）
	 */
	private void failSkill() {
		// HPが足りなくてスキルが使用できない場合のみ、MPのみ消費したいが未實裝（必要ない？）
		// その他の場合は何も消費されない。
		// useConsume(); // HP、MPは減らす
		setCheckedUseSkill(false);
		// テレポートスキル
		if (_skillId == SKILL_TELEPORT || _skillId == SKILL_MASS_TELEPORT || _skillId == SKILL_TELEPORT_TO_MATHER) {
			// テレポートできない場合でも、クライアント側は應答を待っている
			// テレポート待ち狀態の解除（第2引數に意味はない）
			_player.sendPackets(new S_Paralysis(S_Paralysis.TYPE_TELEPORT_UNLOCK, false));
		}
	}

	// ターゲットか？
	private boolean isTarget(L1Character cha) throws Exception {
		boolean _flg = false;

		if (cha instanceof L1PcInstance) {
			L1PcInstance pc = (L1PcInstance) cha;
			if (pc.isGhost() || pc.isGmInvis()) {
				return false;
			}
		}
		if (_targetType == NPC_PC
				&& (cha instanceof L1PcInstance || cha instanceof L1PetInstance || cha instanceof L1SummonInstance)) {
			_flg = true;
		}

		// 破壞不可能なドアは對象外
		if (cha instanceof L1DoorInstance) {
			if (cha.getMaxHp() == 0 || cha.getMaxHp() == 1) {
				return false;
			}
		}

		// マジックドールは對象外
		if (cha instanceof L1DollInstance && _skillId != SKILL_HASTE) {
			return false;
		}

		// 元のターゲットがPet、Summon以外のNPCの場合、PC、Pet、Summonは對象外
		if (_targetType == PC_NPC
				&& _target instanceof L1NpcInstance
				&& !(_target instanceof L1PetInstance)
				&& !(_target instanceof L1SummonInstance)
				&& (cha instanceof L1PetInstance
						|| cha instanceof L1SummonInstance || cha instanceof L1PcInstance)) {
			return false;
		}

		// 元のターゲットがガード以外のNPCの場合、ガードは對象外
		if (_targetType == PC_NPC
				&& _target instanceof L1NpcInstance
				&& !(_target instanceof L1GuardInstance)
				&& cha instanceof L1GuardInstance) {
			return false;
		}

		// NPC對PCでターゲットがモンスターの場合ターゲットではない。
		if ((_skill.getTarget().equals("attack") || _skill.getType() == L1Skills.TYPE_ATTACK)
				&& _targetType == NPC_PC
				&& !(cha instanceof L1PetInstance)
				&& !(cha instanceof L1SummonInstance)
				&& !(cha instanceof L1PcInstance)) {
			return false;
		}

		// NPC對NPCで使用者がMOBで、ターゲットがMOBの場合ターゲットではない。
		if ((_skill.getTarget().equals("attack")
				|| _skill.getType() == L1Skills.TYPE_ATTACK)
				&& _targetType == NPC_NPC
				&& _user instanceof L1MonsterInstance
				&& cha instanceof L1MonsterInstance) {
			return false;
		}

		// 無方向範圍攻擊魔法で攻擊できないNPCは對象外
		if (_skill.getTarget().equals("none")
				&& _skill.getType() == L1Skills.TYPE_ATTACK
				&& (cha instanceof L1AuctionBoardInstance
						|| cha instanceof L1BoardInstance
						|| cha instanceof L1CrownInstance
						|| cha instanceof L1DwarfInstance
						|| cha instanceof L1EffectInstance
						|| cha instanceof L1FieldObjectInstance
						|| cha instanceof L1FurnitureInstance
						|| cha instanceof L1HousekeeperInstance
						|| cha instanceof L1MerchantInstance
						|| cha instanceof L1TeleporterInstance)) {
			return false;
		}

		// 攻擊系スキルで對象が自分は對象外
		if (_skill.getType() == L1Skills.TYPE_ATTACK
				&& cha.getId() == _user.getId()) {
			return false;
		}

		// ターゲットが自分でH-Aの場合效果無し
		if (cha.getId() == _user.getId() && _skillId == SKILL_HEAL_PLEDGE) {
			return false;
		}

		if (((_skill.getTargetTo() & L1Skills.TARGET_TO_PC) == L1Skills.TARGET_TO_PC
				|| (_skill.getTargetTo() & L1Skills.TARGET_TO_CLAN) == L1Skills.TARGET_TO_CLAN || (_skill
				.getTargetTo() & L1Skills.TARGET_TO_PARTY) == L1Skills.TARGET_TO_PARTY)
				&& cha.getId() == _user.getId() && _skillId != SKILL_HEAL_PLEDGE) {
			return true; // ターゲットがパーティーかクラン員のものは自分に效果がある。（ただし、ヒールオールは除外）
		}

		// スキル使用者がPCで、PKモードではない場合、自分のサモン‧ペットは對象外
		if (_user instanceof L1PcInstance
				&& (_skill.getTarget().equals("attack") || _skill.getType() == L1Skills.TYPE_ATTACK)
				&& _isPK == false) {
			if (cha instanceof L1SummonInstance) {
				L1SummonInstance summon = (L1SummonInstance) cha;
				if (_player.getId() == summon.getMaster().getId()) {
					return false;
				}
			} else if (cha instanceof L1PetInstance) {
				L1PetInstance pet = (L1PetInstance) cha;
				if (_player.getId() == pet.getMaster().getId()) {
					return false;
				}
			}
		}

		if ((_skill.getTarget().equals("attack")
				|| _skill.getType() == L1Skills.TYPE_ATTACK)
				&& !(cha instanceof L1MonsterInstance)
				&& _isPK == false
				&& _target instanceof L1PcInstance) {
			L1PcInstance enemy = (L1PcInstance) cha;
			// カウンターディテクション
			if (_skillId == SKILL_COUNTER_DETECTION && enemy.getZoneType() != 1
					&& (cha.hasSkillEffect(SKILL_INVISIBILITY)
							|| cha.hasSkillEffect(SKILL_BLIND_HIDING))) {
				return true; // インビジかブラインドハイディング中
			}
			// 20100614 waja add 魔法相消術 安全區域不可使用
			if (_skillId == SKILL_CANCEL_MAGIC && enemy.getZoneType() != 1) {
				return true;
			}
			if (_player.getClanid() != 0 && enemy.getClanid() != 0) { // クラン所屬中
				// 全戰爭リストを取得
				for (L1War war : L1World.getInstance().getWarList()) {
					if (war.CheckClanInWar(_player.getClanname())) { // 自クランが戰爭に參加中
						if (war.CheckClanInSameWar( // 同じ戰爭に參加中
								_player.getClanname(), enemy.getClanname())) {
							if (L1CastleLocation.checkInAllWarArea(enemy.getX(),
									enemy.getY(), enemy.getMapId())) {
								return true;
							}
						}
					}
				}
			}
			return false; // 攻擊スキルでPKモードじゃない場合
		}

		if (!_user.glanceCheck(cha.getX(), cha.getY()) && _skill.isThrough() == false) {
			// エンチャント、復活スキルは障害物の判定をしない
			if (!(_skill.getType() == L1Skills.TYPE_CHANGE
					|| _skill.getType() == L1Skills.TYPE_RESTORE)) {
				_isGlanceCheckFail = true;
				return false; // 直線上に障害物がある
			}
		}

		if (cha.hasSkillEffect(SKILL_ICE_LANCE)
				&& (_skillId == SKILL_ICE_LANCE || _skillId == SKILL_FREEZING_BLIZZARD
						|| _skillId == SKILL_FREEZING_BREATH)) {
			return false; // アイスランス中にアイスランス、フリージングブリザード、フリージングブレス
		}

		if (cha.hasSkillEffect(SKILL_FREEZING_BLIZZARD)
				&& (_skillId == SKILL_ICE_LANCE || _skillId == SKILL_FREEZING_BLIZZARD
						|| _skillId == SKILL_FREEZING_BREATH)) {
			return false; // フリージングブリザード中にアイスランス、フリージングブリザード、フリージングブレス
		}

		if (cha.hasSkillEffect(SKILL_FREEZING_BREATH)
				&& (_skillId == SKILL_ICE_LANCE || _skillId == SKILL_FREEZING_BLIZZARD
						|| _skillId == SKILL_FREEZING_BREATH)) {
			return false; // フリージングブレス中にアイスランス、フリージングブリザード、フリージングブレス
		}

		if (cha.hasSkillEffect(SKILL_EARTH_BIND) && _skillId == SKILL_EARTH_BIND) {
			return false; // アース バインド中にアース バインド
		}

		if (!(cha instanceof L1MonsterInstance)
				&& (_skillId == SKILL_TAME_MONSTER || _skillId == SKILL_CREATE_ZOMBIE)) {
			return false; // ターゲットがモンスターじゃない（テイミングモンスター）
		}
		if (cha.isDead()
				&& (_skillId != SKILL_CREATE_ZOMBIE
				&& _skillId != SKILL_RESURRECTION
				&& _skillId != SKILL_GREATER_RESURRECTION
				&& _skillId != SKILL_NATURES_MIRACLE)) {
			return false; // ターゲットが死亡している
		}

		if (cha.isDead() == false
				&& (_skillId == SKILL_CREATE_ZOMBIE
				|| _skillId == SKILL_RESURRECTION
				|| _skillId == SKILL_GREATER_RESURRECTION
				|| _skillId == SKILL_NATURES_MIRACLE)) {
			return false; // ターゲットが死亡していない
		}

		if ((cha instanceof L1TowerInstance || cha instanceof L1DoorInstance)
				&& (_skillId == SKILL_CREATE_ZOMBIE
				|| _skillId == SKILL_RESURRECTION
				|| _skillId == SKILL_GREATER_RESURRECTION
				|| _skillId == SKILL_NATURES_MIRACLE)) {
			return false; // ターゲットがガーディアンタワー、ドア
		}

		if (cha instanceof L1PcInstance) {
			L1PcInstance pc = (L1PcInstance) cha;
			if (pc.hasSkillEffect(SKILL_ABSOLUTE_BARRIER)) { // アブソルートバリア中
				if (_skillId == SKILL_CURSE_BLIND
						|| _skillId == SKILL_WEAPON_BREAK
						|| _skillId == SKILL_DARKNESS
						|| _skillId == SKILL_WEAKNESS
						|| _skillId == SKILL_DISEASE
						|| _skillId == SKILL_FOG_OF_SLEEPING
						|| _skillId == SKILL_MASS_SLOW
						|| _skillId == SKILL_SLOW
						|| _skillId == SKILL_CANCEL_MAGIC
						|| _skillId == SKILL_SILENCE
						|| _skillId == SKILL_DECAY_POTION
						|| _skillId == SKILL_MASS_TELEPORT
						|| _skillId == SKILL_DETECTION
						|| _skillId == SKILL_COUNTER_DETECTION
						|| _skillId == SKILL_ERASE_MAGIC
						|| _skillId == SKILL_ENTANGLE
						|| _skillId == SKILL_ENCHANT_DEXTERITY
						|| _skillId == SKILL_ENCHANT_MIGHTY
						|| _skillId == SKILL_BLESS_WEAPON
						|| _skillId == SKILL_EARTH_SKIN
						|| _skillId == SKILL_IMMUNE_TO_HARM
						|| _skillId == SKILL_REMOVE_CURSE) {
					return true;
				} else {
					return false;
				}
			}
		}

		if (cha instanceof L1NpcInstance) {
			int hiddenStatus = ((L1NpcInstance) cha).getHiddenStatus();
			if (hiddenStatus == L1NpcInstance.HIDDEN_STATUS_SINK) {
// BAO提供 解決地龍被無所遁形挖出
				L1NpcInstance npc = (L1NpcInstance) cha;
				int npcId = npc.getNpcTemplate().get_npcId();
//add end
				if (npcId !=45682 && (_skillId == SKILL_DETECTION || _skillId == SKILL_COUNTER_DETECTION)) { // ディテク、Cディテク
//change end
					return true;
				} else {
					return false;
				}
			} else if (hiddenStatus == L1NpcInstance.HIDDEN_STATUS_FLY) {
				return false;
			}
		}

		if ((_skill.getTargetTo() & L1Skills.TARGET_TO_PC) == L1Skills.TARGET_TO_PC // ターゲットがPC
				&& cha instanceof L1PcInstance) {
			_flg = true;
		} else if ((_skill.getTargetTo() & L1Skills.TARGET_TO_NPC) == L1Skills.TARGET_TO_NPC // ターゲットがNPC
				&& (cha instanceof L1MonsterInstance
						|| cha instanceof L1NpcInstance
						|| cha instanceof L1SummonInstance || cha instanceof L1PetInstance)) {
			_flg = true;
		} else if ((_skill.getTargetTo() & L1Skills.TARGET_TO_PET) == L1Skills.TARGET_TO_PET
				&& _user instanceof L1PcInstance) { // ターゲットがSummon,Pet
			if (cha instanceof L1SummonInstance) {
				L1SummonInstance summon = (L1SummonInstance) cha;
				if (summon.getMaster() != null) {
					if (_player.getId() == summon.getMaster().getId()) {
						_flg = true;
					}
				}
			}
			if (cha instanceof L1PetInstance) {
				L1PetInstance pet = (L1PetInstance) cha;
				if (pet.getMaster() != null) {
					if (_player.getId() == pet.getMaster().getId()) {
						_flg = true;
					}
				}
			}
		}

		if (_targetType == PC_PC && cha instanceof L1PcInstance) {
			if ((_skill.getTargetTo() & L1Skills.TARGET_TO_CLAN) == L1Skills.TARGET_TO_CLAN
					&& ((_player.getClanid() != 0 // ターゲットがクラン員
					&& _player.getClanid() == ((L1PcInstance) cha).getClanid()) || _player.isGm())) {
				return true;
			}
			if ((_skill.getTargetTo() & L1Skills.TARGET_TO_PARTY) == L1Skills.TARGET_TO_PARTY
					&& (_player.getParty() // ターゲットがパーティー
							.isMember((L1PcInstance) cha) || _player.isGm())) {
				return true;
			}
		}

		return _flg;
	}

	// ターゲットの一覽を作成
	private void makeTargetList() {
		try {
			if (_skillType == LOGIN) { // ログイン時(死亡時、お化け屋敷のキャンセレーション含む)は使用者のみ
				_targetList.add(new TargetStatus(_user));
				return;
			}
			if (_skill.getTargetTo() == L1Skills.TARGET_TO_ME
					&& (_skill.getType() & L1Skills.TYPE_ATTACK) != L1Skills.TYPE_ATTACK) {
				_targetList.add(new TargetStatus(_user)); // ターゲットは使用者のみ
				return;
			}

			// 射程距離-1の場合は畫面內のオブジェクトが對象
			if (_skill.getRanged() != -1) {
				if (_user.getLocation().getTileLineDistance(
						_target.getLocation()) > _skill.getRanged()) {
					return; // 射程範圍外
				}
			} else {
				if (!_user.getLocation().isInScreen(_target.getLocation())) {
					return; // 射程範圍外
				}
			}

			if (isTarget(_target) == false
					&& !(_skill.getTarget().equals("none"))) {
				// 對象が違うのでスキルが發動しない。
				return;
			}

			if (_skillId == SKILL_LIGHTNING || _skillId == SKILL_FREEZING_BREATH) { // ライトニング、フリージングブレス直線的に範囲を決める
				FastTable<L1Object> al1object = L1World.getInstance().getVisibleLineObjects(_user, _target);

				for (L1Object tgobj : al1object) {
					if (tgobj == null) {
						continue;
					}
					if (!(tgobj instanceof L1Character)) { // ターゲットがキャラクター以外の場合何もしない。
						continue;
					}
					L1Character cha = (L1Character) tgobj;
					if (isTarget(cha) == false) {
						continue;
					}
					_targetList.add(new TargetStatus(cha));
				}
				return;
			}

			if (_skill.getArea() == 0) { // 單体の場合
				if (!_user.glanceCheck(_target.getX(), _target.getY())) { // 直線上に障害物があるか
					if ((_skill.getType() & L1Skills.TYPE_ATTACK) == L1Skills
							.TYPE_ATTACK && _skillId != 10026
							&& _skillId != 10027 && _skillId != 10028
							&& _skillId != 10029) { // 安息攻撃以外の攻撃スキル
						_targetList.add(new TargetStatus(_target, false)); // ダメージも発生しないし、ダメージモーションも発生しないが、スキルは発動
						return;
					}
				}
				_targetList.add(new TargetStatus(_target));
			} else { // 範圍の場合
				if (!_skill.getTarget().equals("none")) {
					_targetList.add(new TargetStatus(_target));
				}

				if (_skillId != 49
						&& !(_skill.getTarget().equals("attack") || _skill
								.getType() == L1Skills.TYPE_ATTACK)) {
					// 攻擊系以外のスキルとH-A以外はターゲット自身を含める
					_targetList.add(new TargetStatus(_user));
				}

				List<L1Object> objects;
				if (_skill.getArea() == -1) {
					objects = L1World.getInstance().getVisibleObjects(_user);
				} else {
					objects = L1World.getInstance().getVisibleObjects(_target, _skill.getArea());
				}
				for (L1Object tgobj : objects) {
					if (tgobj == null) {
						continue;
					}
					if (!(tgobj instanceof L1Character)) { // ターゲットがキャラクター以外の場合何もしない。
						continue;
					}
					L1Character cha = (L1Character) tgobj;
					if (!isTarget(cha)) {
						continue;
					}

					_targetList.add(new TargetStatus(cha));
				}
				return;
			}

		} catch (Exception e) {
			_log.finest("exception in L1Skilluse makeTargetList" + e);
		}
	}

	// メッセージの表示（何か起こったとき）
	private void sendHappenMessage(L1PcInstance pc) {
		int happenMsgId = _skill.getSysmsgIdHappen();
		if (happenMsgId > 0) {
			SystemMessageId msgId = SystemMessageId.getSystemMessageId(happenMsgId);
			pc.sendPackets(new S_ServerMessage(msgId));
		}
	}

	// 失敗メッセージ表示のハンドル
	private void sendFailMessageHandle() {
		// 攻擊スキル以外で對象を指定するスキルが失敗した場合は失敗したメッセージをクライアントに送信
		// ※攻擊スキルは障害物があっても成功時と同じアクションであるべき。
		if (_skill.getType() != L1Skills.TYPE_ATTACK
				&& !_skill.getTarget().equals("none")
				&& _targetList.size() == 0) {
			sendFailMessage();
		}
	}

	// メッセージの表示（失敗したとき）
	private void sendFailMessage() {
		int failMsgId = _skill.getSysmsgIdFail();
		if (failMsgId > 0 && (_user instanceof L1PcInstance)) {
			SystemMessageId msgID = SystemMessageId.getSystemMessageId(failMsgId);
			_player.sendPackets(new S_ServerMessage(msgID));
		}
	}

	// 精霊魔法の属性と使用者の属性は一致するか？（とりあえずの対処なので、対応できたら消去して下さい)
	private boolean isAttrAgrees() {
		int magicattr = _skill.getAttr();
		if (_user instanceof L1NpcInstance) { // NPCが使った場合なんでもOK
			return true;
		}

		if (_skill.getSkillLevel() >= 17 && _skill.getSkillLevel() <= 22
				&& magicattr != 0 // 精霊魔法で、無属性魔法ではなく、
				&& magicattr != _player.getElfAttr() // 使用者と魔法の属性が一致しない。
				&& !_player.isGm()) { // ただしGMは例外
			return false;
		}
		return true;
	}

	/**
	 * スキルを使用するために必要なHPがあるか返す。
	 * 
	 * @return HPが十分であればtrue
	 */
	private boolean isEnoughHp() {
		return false;
	}

	/**
	 * スキルを使用するために必要なMPがあるか返す。
	 * 
	 * @return MPが十分であればtrue
	 */
	private boolean isEnoughMp() {
		return false;
	}

	// 必要ＨＰ、ＭＰがあるか？
	private boolean isHPMPConsume() {
		_mpConsume = _skill.getMpConsume();
		_hpConsume = _skill.getHpConsume();
		int currentMp = 0;
		int currentHp = 0;

		if (_user instanceof L1NpcInstance) {
			currentMp = _npc.getCurrentMp();
			currentHp = _npc.getCurrentHp();
		} else {
			currentMp = _player.getCurrentMp();
			currentHp = _player.getCurrentHp();

			// MPのINT輕減
			if (_player.getInt() > 12
					&& _skillId > SKILL_HOLY_WEAPON
					&& _skillId <= SKILL_FREEZING_BLIZZARD) { // LV2以上
				_mpConsume--;
			}
			if (_player.getInt() > 13
					&& _skillId > SKILL_STALAC
					&& _skillId <= SKILL_FREEZING_BLIZZARD) { // LV3以上
				_mpConsume--;
			}
			if (_player.getInt() > 14
					&& _skillId > SKILL_REVEAL_WEAKNESS
					&& _skillId <= SKILL_FREEZING_BLIZZARD) { // LV4以上
				_mpConsume--;
			}
			if (_player.getInt() > 15
					&& _skillId > SKILL_MEDITATION
					&& _skillId <= SKILL_FREEZING_BLIZZARD) { // LV5以上
				_mpConsume--;
			}
			if (_player.getInt() > 16
					&& _skillId > SKILL_DARKNESS
					&& _skillId <= SKILL_FREEZING_BLIZZARD) { // LV6以上
				_mpConsume--;
			}
			if (_player.getInt() > 17
					&& _skillId > SKILL_BLESS_WEAPON
					&& _skillId <= SKILL_FREEZING_BLIZZARD) { // LV7以上
				_mpConsume--;
			}
			if (_player.getInt() > 18
					&& _skillId > SKILL_DISEASE
					&& _skillId <= SKILL_FREEZING_BLIZZARD) { // LV8以上
				_mpConsume--;
			}

			if (_player.getInt() > 12
					&& _skillId >= SKILL_STUN_SHOCK && _skillId <= SKILL_COUNTER_BARRIER) {
				_mpConsume -= (_player.getInt() - 12);
			}

			// MPの裝備輕減
			if (_skillId == SKILL_ENCHANT_DEXTERITY
					&& _player.getInventory().checkEquipped(20013)) { // 迅速ヘルム裝備中にPE:DEX
				_mpConsume /= 2;
			}
			if (_skillId == SKILL_HASTE
					&& _player.getInventory().checkEquipped(20013)) { // 迅速ヘルム裝備中にヘイスト
				_mpConsume /= 2;
			}
			if (_skillId == SKILL_LESSER_HEAL
					&& _player.getInventory().checkEquipped(20014)) { // 治癒ヘルム裝備中にヒール
				_mpConsume /= 2;
			}
			if (_skillId == SKILL_HEAL
					&& _player.getInventory().checkEquipped(20014)) { // 治癒ヘルム裝備中にエキストラヒール
				_mpConsume /= 2;
			}
			if (_skillId == SKILL_ENCHANT_WEAPON
					&& _player.getInventory().checkEquipped(20015)) { // 力ヘルム裝備中にエンチャントウエポン
				_mpConsume /= 2;
			}
			if (_skillId == SKILL_DETECTION
					&& _player.getInventory().checkEquipped(20015)) { // 力ヘルム裝備中にディテクション
				_mpConsume /= 2;
			}
			if (_skillId == SKILL_ENCHANT_MIGHTY
					&& _player.getInventory().checkEquipped(20015)) { // 力ヘルム裝備中にPE:STR
				_mpConsume /= 2;
			}
			if (_skillId == SKILL_HASTE
					&& _player.getInventory().checkEquipped(20008)) { // マイナーウィンドヘルム裝備中にヘイスト
				_mpConsume /= 2;
			}
			if (_skillId == SKILL_GREATER_HASTE
					&& _player.getInventory().checkEquipped(20023)) { // ウィンドヘルム裝備中にグレーターヘイスト
				_mpConsume /= 2;
			}

			if (0 < _skill.getMpConsume()) { // MPを消費するスキルであれば
				_mpConsume = Math.max(_mpConsume, 1); // 最低でも1消費する。
			}

			// MPのオリジナルINT輕減
			if (_player.getOriginalMagicConsumeReduction() > 0) {
				_mpConsume -= _player.getOriginalMagicConsumeReduction();
			// 20100523 waja add hms fix code ( 智力減免2的王族騎士戴治盔初治不扣 mp )
			if (_mpConsume <= 0) { // 智力減免後的最低耗魔量為1
				_mpConsume = 1;
			}
			// add end
			}
		}

		if (currentHp < _hpConsume + 1) {
			if (_user instanceof L1PcInstance) {
				_player.sendPackets(new S_ServerMessage(SystemMessageId.$279));
			}
			return false;
		} else if (currentMp < _mpConsume) {
			if (_user instanceof L1PcInstance) {
				_player.sendPackets(new S_ServerMessage(SystemMessageId.$278));
			}
			return false;
		}

		return true;
	}

	// 必要材料があるか？
	private boolean isItemConsume() {

		int itemConsume = _skill.getItemConsumeId();
		int itemConsumeCount = _skill.getItemConsumeCount();

		if (itemConsume == 0) {
			return true; // 材料を必要としない魔法
		}

		if (!_player.getInventory().checkItem(itemConsume, itemConsumeCount)) {
			return false; // 必要材料が足りなかった。
		}

		return true;
	}

	// 使用材料、HP‧MP、Lawfulをマイナスする。
	private void useConsume() {
		if (_user instanceof L1NpcInstance) {
			// NPCの場合、HP、MPのみマイナス
			int current_hp = _npc.getCurrentHp() - _hpConsume;
			_npc.setCurrentHp(current_hp);

			int current_mp = _npc.getCurrentMp() - _mpConsume;
			_npc.setCurrentMp(current_mp);
			return;
		}

		// HP‧MPをマイナス
		if (isHPMPConsume()) {
			if (_skillId == SKILL_FINAL_BURN) { // ファイナル バーン
				_player.setCurrentHp(1);
				_player.setCurrentMp(0);
			} else {
				int current_hp = _player.getCurrentHp() - _hpConsume;
				_player.setCurrentHp(current_hp);

				int current_mp = _player.getCurrentMp() - _mpConsume;
				_player.setCurrentMp(current_mp);
			}
		}

		// Lawfulをマイナス
		int lawful = _player.getLawful() + _skill.getLawful();
		if (lawful > 32767) {
			lawful = 32767;
		}
		if (lawful < -32767) {
			lawful = -32767;
		}
		_player.setLawful(lawful);

		int itemConsume = _skill.getItemConsumeId();
		int itemConsumeCount = _skill.getItemConsumeCount();

		if (itemConsume == 0) {
			return; // 材料を必要としない魔法
		}

		// 使用材料をマイナス
		_player.getInventory().consumeItem(itemConsume, itemConsumeCount);
	}

	// マジックリストに追加する。
	private void addMagicList(L1Character cha, boolean repetition) {
		if (_skillTime == 0) {
			_getBuffDuration = _skill.getBuffDuration() * 1000; // 效果時間
			if (_skill.getBuffDuration() == 0) {
				if (_skillId == SKILL_INVISIBILITY) { // インビジビリティ
					cha.setSkillEffect(SKILL_INVISIBILITY, 0);
				}
				return;
			}
		} else {
			_getBuffDuration = _skillTime * 1000; // パラメータのtimeが0以外なら、效果時間として設定する
		}

		if (_skillId == SKILL_STUN_SHOCK) {
			_getBuffDuration = _shockStunDuration;
		}

		if (_skillId == SKILL_CURSE_POISON) { // カーズポイズンの效果處理はL1Poisonに移讓。
			return;
		}
		if (_skillId == SKILL_CURSE_PARALYZE
				|| _skillId == CURSE_PARALYZE2) { // カーズパラライズの效果處理はL1CurseParalysisに移讓。
			return;
		}
		if (_skillId == SKILL_POLYMORPH) { // シェイプチェンジの效果處理はL1PolyMorphに移讓。
			return;
		}
		if (_skillId == SKILL_BLESSED_ARMOR || _skillId == SKILL_HOLY_WEAPON // 武器‧防具に效果がある處理はL1ItemInstanceに移讓。
				|| _skillId == SKILL_ENCHANT_WEAPON || _skillId == SKILL_BLESS_WEAPON
				|| _skillId == SKILL_SHADOW_FANG) {
			return;
		}
		if ((_skillId == SKILL_ICE_LANCE || _skillId == SKILL_FREEZING_BLIZZARD
				|| _skillId == SKILL_FREEZING_BREATH) && !_isFreeze) { // 凍結失敗
			return;
		}
		if (_skillId == SKILL_AWAKEN_ANTHARAS || _skillId == SKILL_AWAKEN_FAFURION
				|| _skillId == SKILL_AWAKEN_VALAKAS) { // 覺醒の效果處理はL1Awakeに移讓。
			return;
		}

		cha.setSkillEffect(_skillId, _getBuffDuration);

		if (cha instanceof L1PcInstance && repetition) { // 對象がPCで既にスキルが重複している場合
			L1PcInstance pc = (L1PcInstance) cha;
			sendIcon(pc);
		}
	}

	// アイコンの送信
	private void sendIcon(L1PcInstance pc) {
		if (_skillTime == 0) {
			_getBuffIconDuration = _skill.getBuffDuration(); // 效果時間
		} else {
			_getBuffIconDuration = _skillTime; // パラメータのtimeが0以外なら、效果時間として設定する
		}

		if (_skillId == SKILL_SHIELD) { // シールド
			pc.sendPackets(new S_SkillIconShield(5, _getBuffIconDuration));
		} else if (_skillId == SKILL_SHADOW_ARMOR) { // シャドウ アーマー
			pc.sendPackets(new S_SkillIconShield(3, _getBuffIconDuration));
		} else if (_skillId == SKILL_DRESS_DEXTERITY) { // ドレス デクスタリティー
			pc.sendPackets(new S_DexUp(pc, 2, _getBuffIconDuration));
		} else if (_skillId == SKILL_DRESS_MIGHTY) { // ドレス マイティー
			pc.sendPackets(new S_StrUp(pc, 2, _getBuffIconDuration));
		} else if (_skillId == SKILL_GLOWING_AURA) { // グローウィング オーラ
			pc.sendPackets(new S_SkillIconAura(113, _getBuffIconDuration));
		} else if (_skillId == SKILL_SHINING_AURA) { // シャイニング オーラ
			pc.sendPackets(new S_SkillIconAura(114, _getBuffIconDuration));
		} else if (_skillId == SKILL_BRAVE_AURA) { // ブレイブ オーラ
			pc.sendPackets(new S_SkillIconAura(116, _getBuffIconDuration));
		} else if (_skillId == SKILL_FIRE_WEAPON) { // ファイアー ウェポン
			pc.sendPackets(new S_SkillIconAura(147, _getBuffIconDuration));
		} else if (_skillId == SKILL_WIND_SHOT) { // ウィンド ショット
			pc.sendPackets(new S_SkillIconAura(148, _getBuffIconDuration));
		} else if (_skillId == SKILL_BLESS_OF_FIRE) { // ファイアー ブレス
			pc.sendPackets(new S_SkillIconAura(154, _getBuffIconDuration));
		} else if (_skillId == SKILL_EYE_OF_STORM) { // ストーム アイ
			pc.sendPackets(new S_SkillIconAura(155, _getBuffIconDuration));
		} else if (_skillId == SKILL_BLESS_OF_EARTH) { // アース ブレス
			pc.sendPackets(new S_SkillIconShield(7, _getBuffIconDuration));
		} else if (_skillId == SKILL_BURNING_WEAPON) { // バーニング ウェポン
			pc.sendPackets(new S_SkillIconAura(162, _getBuffIconDuration));
		} else if (_skillId == SKILL_STORM_SHOT) { // ストーム ショット
			pc.sendPackets(new S_SkillIconAura(165, _getBuffIconDuration));
		} else if (_skillId == SKILL_IRON_SKIN) { // アイアン スキン
			pc.sendPackets(new S_SkillIconShield(10, _getBuffIconDuration));
		} else if (_skillId == SKILL_EARTH_SKIN) { // アース スキン
			pc.sendPackets(new S_SkillIconShield(6, _getBuffIconDuration));
		} else if (_skillId == SKILL_ENCHANT_MIGHTY) { // フィジカル エンチャント：STR
			pc.sendPackets(new S_StrUp(pc, 5, _getBuffIconDuration));
		} else if (_skillId == SKILL_ENCHANT_DEXTERITY) { // フィジカル エンチャント：DEX
			pc.sendPackets(new S_DexUp(pc, 5, _getBuffIconDuration));
		} else if (_skillId == SKILL_HASTE || _skillId == SKILL_GREATER_HASTE) { // グレーターヘイスト
			pc.sendPackets(new S_SkillHaste(pc.getId(), 1, _getBuffIconDuration));
			pc.broadcastPacket(new S_SkillHaste(pc.getId(), 1, 0));
		} else if (_skillId == SKILL_HOLY_WALK
				|| _skillId == SKILL_MOVING_ACCELERATION || _skillId == SKILL_WIND_WALK) { // ホーリーウォーク、ムービングアクセレーション、ウィンドウォーク
			pc.sendPackets(new S_SkillBrave(pc.getId(), 4, _getBuffIconDuration));
			pc.broadcastPacket(new S_SkillBrave(pc.getId(), 4, 0));
		} else if (_skillId == SKILL_BLOODLUST) { // ブラッドラスト
			pc.sendPackets(new S_SkillBrave(pc.getId(), 6, _getBuffIconDuration));
			pc.broadcastPacket(new S_SkillBrave(pc.getId(), 6, 0));
		} else if (_skillId == SKILL_SLOW
				|| _skillId == SKILL_MASS_SLOW || _skillId == SKILL_ENTANGLE) { // スロー、エンタングル、マススロー
			pc.sendPackets(new S_SkillHaste(pc.getId(), 2, _getBuffIconDuration));
			pc.broadcastPacket(new S_SkillHaste(pc.getId(), 2, 0));
		} else if (_skillId == SKILL_IMMUNE_TO_HARM) {
			pc.sendPackets(new S_SkillIconGFX(40, _getBuffIconDuration));
		}
		pc.sendPackets(new S_OwnCharStatus(pc));
	}

	// グラフィックの送信
	private void sendGrfx(boolean isSkillAction) {
		int actionId = _skill.getActionId();
		int castgfx = _skill.getCastGfx();
		if (castgfx == 0) {
			return; // 表示するグラフィックが無い
		}

		if (_user instanceof L1PcInstance) {
			if (_skillId == SKILL_FIRE_WALL || _skillId == SKILL_LIFE_STREAM) {
				L1PcInstance pc = (L1PcInstance) _user;
				if (_skillId == SKILL_FIRE_WALL) {
					pc.setHeading(pc.targetDirection(_targetX, _targetY));
					pc.sendPackets(new S_ChangeHeading(pc));
					pc.broadcastPacket(new S_ChangeHeading(pc));
				}
				S_DoActionGFX gfx = new S_DoActionGFX(pc.getId(), actionId);
				pc.sendPackets(gfx);
				pc.broadcastPacket(gfx);
				return;
			}

			int targetid = _target.getId();

			if (_skillId == SKILL_STUN_SHOCK) {
				if (_targetList.size() == 0) { // 失敗
					return;
				} else {
					if (_target instanceof L1PcInstance) {
						L1PcInstance pc = (L1PcInstance) _target;
						pc.sendPackets(new S_SkillSound(pc.getId(), 4434));
						pc.broadcastPacket(new S_SkillSound(pc.getId(), 4434));
					} else if (_target instanceof L1NpcInstance) {
						_target.broadcastPacket(new S_SkillSound(_target.getId(), 4434));
					}
					return;
				}
			}

			if (_skillId == SKILL_LIGHT) {
				L1PcInstance pc = (L1PcInstance) _target;
				pc.sendPackets(new S_Sound(145));
			}

			if (_targetList.size() == 0 && !(_skill.getTarget().equals("none"))) {
				// ターゲット數が０で對象を指定するスキルの場合、魔法使用エフェクトだけ表示して終了
				int tempchargfx = _player.getTempCharGfx();
				if (tempchargfx == 5727 || tempchargfx == 5730) { // シャドウ系變身のモーション對應
					actionId = ActionCodes.ACTION_SkillBuff;
				} else if (tempchargfx == 5733 || tempchargfx == 5736) {
					actionId = ActionCodes.ACTION_Attack;
				}
				if (isSkillAction) {
					S_DoActionGFX gfx = new S_DoActionGFX(_player.getId(), actionId);
					_player.sendPackets(gfx);
					_player.broadcastPacket(gfx);
				}
				return;
			}

			if (_skill.getTarget().equals("attack") && _skillId != 18) {
				if (isPcSummonPet(_target)) { // 對象がPC、サモン、ペット
					if (_player.getZoneType() == 1
							|| _target.getZoneType() == 1 // 攻擊する側または攻擊される側がセーフティーゾーン
							|| _player.checkNonPvP(_player, _target)) { // Non-PvP設定
						_player.sendPackets(new S_UseAttackSkill(_player, 0, castgfx, _targetX, _targetY, actionId)); // ターゲットへのモーションはなし
						_player.broadcastPacket(new S_UseAttackSkill(_player, 0, castgfx, _targetX, _targetY, actionId));
						return;
					}
				}

				if (_skill.getArea() == 0) { // 單体攻擊魔法
					_player.sendPackets(new S_UseAttackSkill(_player, targetid, castgfx, _targetX, _targetY, actionId));
					_player.broadcastPacket(new S_UseAttackSkill(_player, targetid, castgfx, _targetX, _targetY, actionId));
					_target.broadcastPacketExceptTargetSight(new S_DoActionGFX(targetid, ActionCodes.ACTION_Damage), _player);
				} else { // 有方向範圍攻擊魔法
					L1Character[] cha = new L1Character[_targetList.size()];
					int i = 0;
					for (TargetStatus ts : _targetList) {
						cha[i] = ts.getTarget();
						i++;
					}
					_player.sendPackets(new S_RangeSkill(_player, cha, castgfx, actionId, S_RangeSkill.TYPE_DIR));
					_player.broadcastPacket(new S_RangeSkill(_player, cha, castgfx, actionId, S_RangeSkill.TYPE_DIR));
				}
			} else if (_skill.getTarget().equals("none") && _skill.getType() ==
					L1Skills.TYPE_ATTACK) { // 無方向範圍攻擊魔法
				L1Character[] cha = new L1Character[_targetList.size()];
				int i = 0;
				for (TargetStatus ts : _targetList) {
					cha[i] = ts.getTarget();
					cha[i].broadcastPacketExceptTargetSight(new S_DoActionGFX(
							cha[i].getId(), ActionCodes.ACTION_Damage), _player);
					i++;
				}
				_player.sendPackets(new S_RangeSkill(_player, cha, castgfx, actionId, S_RangeSkill.TYPE_NODIR));
				_player.broadcastPacket(new S_RangeSkill(_player, cha, castgfx, actionId, S_RangeSkill.TYPE_NODIR));
			} else { // 補助魔法
				// テレポート、マステレ、テレポートトゥマザー以外
				if (_skillId != 5 && _skillId != 69 && _skillId != 131) {
					// 魔法を使う動作のエフェクトは使用者だけ
					if (isSkillAction) {
						S_DoActionGFX gfx = new S_DoActionGFX(_player.getId(),
								_skill.getActionId());
						_player.sendPackets(gfx);
						_player.broadcastPacket(gfx);
					}
					if (_skillId == SKILL_COUNTER_MAGIC
							|| _skillId == SKILL_COUNTER_BARRIER
							|| _skillId == SKILL_COUNTER_MIRROR) {
						_player.sendPackets(new S_SkillSound(targetid, castgfx));
					} else if (_skillId == SKILL_TRUE_TARGET) { // トゥルーターゲットは個別處理で送信濟
						return;
					} else if (_skillId == SKILL_AWAKEN_ANTHARAS // 覺醒：アンタラス
							|| _skillId == SKILL_AWAKEN_FAFURION // 覺醒：パプリオン
							|| _skillId == SKILL_AWAKEN_VALAKAS) { // 覺醒：ヴァラカス
						if (_skillId == _player.getAwakeSkillId()) { // 再詠唱なら解除でエフェクトなし
							_player.sendPackets(new S_SkillSound(targetid, castgfx));
							_player.broadcastPacket(new S_SkillSound(targetid, castgfx));
						} else {
							return;
						}
					} else {
						_player.sendPackets(new S_SkillSound(targetid, castgfx));
						_player.broadcastPacket(new S_SkillSound(targetid, castgfx));
					}
				}

				// スキルのエフェクト表示はターゲット全員だが、あまり必要性がないので、ステータスのみ送信
				for (TargetStatus ts : _targetList) {
					L1Character cha = ts.getTarget();
					if (cha instanceof L1PcInstance) {
						L1PcInstance pc = (L1PcInstance) cha;
						pc.sendPackets(new S_OwnCharStatus(pc));
					}
				}
			}
		} else if (_user instanceof L1NpcInstance) { // NPCがスキルを使った場合
			int targetid = _target.getId();

			if (_user instanceof L1MerchantInstance) {
				_user.broadcastPacket(new S_SkillSound(targetid, castgfx));
				return;
			}

			if (_targetList.size() == 0 && !(_skill.getTarget().equals("none"))) {
				// ターゲット數が０で對象を指定するスキルの場合、魔法使用エフェクトだけ表示して終了
				S_DoActionGFX gfx = new S_DoActionGFX(_user.getId(), _skill.getActionId());
				_user.broadcastPacket(gfx);
				return;
			}

			if (_skill.getTarget().equals("attack") && _skillId != 18) {
				if (_skill.getArea() == 0) { // 單体攻擊魔法
					_user.broadcastPacket(new S_UseAttackSkill(_user, targetid, castgfx, _targetX, _targetY, actionId));
					_target.broadcastPacketExceptTargetSight(new S_DoActionGFX( targetid, ActionCodes.ACTION_Damage), _user);
				} else { // 有方向範圍攻擊魔法
					L1Character[] cha = new L1Character[_targetList.size()];
					int i = 0;
					for (TargetStatus ts : _targetList) {
						cha[i] = ts.getTarget();
						cha[i].broadcastPacketExceptTargetSight(new S_DoActionGFX(cha[i].getId(), ActionCodes.ACTION_Damage), _user);
						i++;
					}
					_user.broadcastPacket(new S_RangeSkill(_user, cha,castgfx, actionId, S_RangeSkill.TYPE_DIR));
				}
			} else if (_skill.getTarget().equals("none") && _skill.getType() == L1Skills.TYPE_ATTACK) { // 無方向範圍攻擊魔法
				L1Character[] cha = new L1Character[_targetList.size()];
				int i = 0;
				for (TargetStatus ts : _targetList) {
					cha[i] = ts.getTarget();
					i++;
				}
				_user.broadcastPacket(new S_RangeSkill(_user, cha, castgfx, actionId, S_RangeSkill.TYPE_NODIR));
			} else { // 補助魔法
				// テレポート、マステレ、テレポートトゥマザー以外
				if (_skillId != 5 && _skillId != 69 && _skillId != 131) {
					// 魔法を使う動作のエフェクトは使用者だけ
					S_DoActionGFX gfx = new S_DoActionGFX(_user.getId(), _skill.getActionId());
					_user.broadcastPacket(gfx);
					_user.broadcastPacket(new S_SkillSound(targetid, castgfx));
				}
			}
		}
	}

	// 重複できないスキルの削除
	// 例：ファイア ウェポンとバーニングウェポンなど
	private void deleteRepeatedSkills(L1Character cha) {
		final int[][] repeatedSkills = {
				// ホーリー ウェポン、エンチャント ウェポン、ブレス ウェポン, シャドウ ファング
				// これらはL1ItemInstanceで管理
// { HOLY_WEAPON, ENCHANT_WEAPON, BLESS_WEAPON, SHADOW_FANG },
				// ファイアー ウェポン、ウィンド ショット、ファイアー ブレス、ストーム アイ、バーニング ウェポン、ストーム ショット
				{ SKILL_FIRE_WEAPON, SKILL_WIND_SHOT, SKILL_BLESS_OF_FIRE, SKILL_EYE_OF_STORM,
					SKILL_BURNING_WEAPON, SKILL_STORM_SHOT },
				// シールド、シャドウ アーマー、アース スキン、アースブレス、アイアン スキン
				{ SKILL_SHIELD, SKILL_SHADOW_ARMOR, SKILL_EARTH_SKIN, SKILL_BLESS_OF_EARTH, SKILL_IRON_SKIN },
				// ホーリー ウォーク、ムービング アクセレーション、ウィンド ウォーク、BP、ワッフル、ユグドラの実、ブラッドラスト
				{ SKILL_HOLY_WALK, SKILL_MOVING_ACCELERATION, SKILL_WIND_WALK, STATUS_BRAVE,
						STATUS_ELFBRAVE, STATUS_RIBRAVE, SKILL_BLOODLUST },
				// ヘイスト、グレーター ヘイスト、GP
				{ SKILL_HASTE, SKILL_GREATER_HASTE, STATUS_HASTE },
				// フィジカル エンチャント：DEX、ドレス デクスタリティー
				{ SKILL_ENCHANT_DEXTERITY, SKILL_DRESS_DEXTERITY },
				// フィジカル エンチャント：STR、ドレス マイティー
				{ SKILL_ENCHANT_MIGHTY, SKILL_DRESS_MIGHTY },
				// グローウィングオーラ、シャイニングオーラ
				{ SKILL_GLOWING_AURA, SKILL_SHINING_AURA } };

		for (int[] skills : repeatedSkills) {
			for (int id : skills) {
				if (id == _skillId) {
					stopSkillList(cha, skills);
				}
			}
		}
	}

	// 重複しているスキルを一旦すべて削除
	private void stopSkillList(L1Character cha, int[] repeat_skill) {
		for (int skillId : repeat_skill) {
			if (skillId != _skillId) {
				cha.removeSkillEffect(skillId);
			}
		}
	}

	// ディレイの設定
	private void setDelay() {
		if (_skill.getReuseDelay() > 0) {
			SkillDelay.onSkillUse(_user, _skill.getReuseDelay());
		}
	}

	private void runSkill() {

		if (_skillId == SKILL_LIFE_STREAM) {
			L1EffectSpawn.getInstance().spawnEffect(81169,
					_skill.getBuffDuration() * 1000,
					_targetX, _targetY, _user.getMapId());
			return;
		} else if (_skillId == SKILL_CUBE_IGNITION) {
			L1EffectSpawn.getInstance().spawnEffect(80149,
					_skill.getBuffDuration() * 1000,
					_targetX, _targetY, _user.getMapId(),
					(L1PcInstance) _user, _skillId);
			return;
		} else if (_skillId == SKILL_CUBE_QUAKE) {
			L1EffectSpawn.getInstance().spawnEffect(80150,
					_skill.getBuffDuration() * 1000,
					_targetX, _targetY, _user.getMapId(),
					(L1PcInstance) _user, _skillId);
			return;
		} else if (_skillId == SKILL_CUBE_SHOCK) {
			L1EffectSpawn.getInstance().spawnEffect(80151,
					_skill.getBuffDuration() * 1000,
					_targetX, _targetY, _user.getMapId());
			return;
		} else if (_skillId == SKILL_CUBE_BALANCE) {
			L1EffectSpawn.getInstance().spawnEffect(80152,
					_skill.getBuffDuration() * 1000,
					_targetX, _targetY, _user.getMapId(),
					(L1PcInstance) _user, _skillId);
			return;
		}

		if (_skillId == SKILL_FIRE_WALL) { // ファイアーウォール
			L1EffectSpawn.getInstance().doSpawnFireWall(_user, _targetX, _targetY);
			return;
		}

		// カウンターマジック有/無效の設定
		for (int skillId : EXCEPT_COUNTER_MAGIC) {
			if (_skillId == skillId) {
				_isCounterMagic = false; // カウンターマジック無效
				break;
			}
		}

		// NPCにショックスタンを使用させるとonActionでNullPointerExceptionが發生するため
		// とりあえずPCが使用した時のみ
		if (_skillId == SKILL_STUN_SHOCK && _user instanceof L1PcInstance) {
			_target.onAction(_player);
		}

		if (!isTargetCalc(_target)) {
			return;
		}

		try {
			TargetStatus ts = null;
			L1Character cha = null;
			int dmg = 0;
			int drainMana = 0;
			int heal = 0;
			boolean isSuccess = false;
			int undeadType = 0;

			for (Iterator<TargetStatus> iter = _targetList.iterator(); iter.hasNext();) {
				ts = null;
				cha = null;
				dmg = 0;
				heal = 0;
				isSuccess = false;
				undeadType = 0;

				ts = iter.next();
				cha = ts.getTarget();

				if (!ts.isCalc() || !isTargetCalc(cha)) {
					continue; // 計算する必要がない。
				}

				L1Magic _magic = new L1Magic(_user, cha);
				_magic.setLeverage(getLeverage());

				if (cha instanceof L1MonsterInstance) { // アンデットの判定
					undeadType = ((L1MonsterInstance) cha).getNpcTemplate().get_undead();
				}

				// 確率系スキルで失敗が確定している場合
				if ((_skill.getType() == L1Skills.TYPE_CURSE || _skill
						.getType() == L1Skills.TYPE_PROBABILITY)
						&& isTargetFailure(cha)) {
					iter.remove();
					continue;
				}

				if (cha instanceof L1PcInstance) { // ターゲットがPCの場合のみアイコンは送信する。
					if (_skillTime == 0) {
						_getBuffIconDuration = _skill.getBuffDuration(); // 效果時間
					} else {
						_getBuffIconDuration = _skillTime; // パラメータのtimeが0以外なら、效果時間として設定する
					}
				}

				deleteRepeatedSkills(cha); // 重複したスキルの削除

				if (_skill.getType() == L1Skills.TYPE_ATTACK
						&& _user.getId() != cha.getId()) { // 攻擊系スキル＆ターゲットが使用者以外であること。
					if (isUseCounterMagic(cha)) { // カウンターマジックが發動した場合、リストから削除
						iter.remove();
						continue;
					}
					dmg = _magic.calcMagicDamage(_skillId);
					cha.removeSkillEffect(SKILL_ERASE_MAGIC); // イレースマジック中なら、攻擊魔法で解除
				} else if (_skill.getType() == L1Skills.TYPE_CURSE
						|| _skill.getType() == L1Skills.TYPE_PROBABILITY) { // 確率系スキル
					isSuccess = _magic.calcProbabilityMagic(_skillId);
					if (_skillId != SKILL_ERASE_MAGIC) {
						cha.removeSkillEffect(SKILL_ERASE_MAGIC); // イレースマジック中なら、確率魔法で解除
					}
					if (_skillId != SKILL_FOG_OF_SLEEPING) {
						cha.removeSkillEffect(SKILL_FOG_OF_SLEEPING); // フォグオブスリーピング中なら、確率魔法で解除
					}
					if (isSuccess) { // 成功したがカウンターマジックが發動した場合、リストから削除
						if (isUseCounterMagic(cha)) { // カウンターマジックが發動したか
							iter.remove();
							continue;
						}
					} else { // 失敗した場合、リストから削除
						if (_skillId == SKILL_FOG_OF_SLEEPING
								&& cha instanceof L1PcInstance) {
							L1PcInstance pc = (L1PcInstance) cha;
							pc.sendPackets(new S_ServerMessage(SystemMessageId.$297));
						}
						iter.remove();
						continue;
					}
				} else if (_skill.getType() == L1Skills.TYPE_HEAL) { // 回復系スキル
					// 回復量はマイナスダメージで表現
					dmg = -1 * _magic.calcHealing(_skillId);
					if (cha.hasSkillEffect(SKILL_WATER_LIFE)) { // ウォーターライフ中は回復量２倍
						dmg *= 2;
					}
					if (cha.hasSkillEffect(SKILL_POLLUTE_WATER)) { // ポルートウォーター中は回復量1/2倍
						dmg /= 2;
					}
				}

				// ■■■■ 個別處理のあるスキルのみ書いてください。 ■■■■

				// すでにスキルを使用濟みの場合なにもしない
				// ただしショックスタンは重ねがけ出來るため例外
				if (cha.hasSkillEffect(_skillId) && _skillId != SKILL_STUN_SHOCK) {
					addMagicList(cha, true); // ターゲットに魔法の效果時間を上書き
					if (_skillId != SKILL_POLYMORPH) { // シェイプ チェンジは變身を上書き出來るため例外
						continue;
					}
				}

				// ●●●● PC、NPC兩方效果のあるスキル ●●●●
				if (_skillId == SKILL_HASTE) { // ヘイスト
					if (cha.getMoveSpeed() != 2) { // スロー中以外
						if (cha instanceof L1PcInstance) {
							L1PcInstance pc = (L1PcInstance) cha;
							if (pc.getHasteItemEquipped() > 0) {
								continue;
							}
							pc.setDrink(false);
							pc.sendPackets(new S_SkillHaste(pc.getId(), 1, _getBuffIconDuration));
						}
						cha.broadcastPacket(new S_SkillHaste(cha.getId(), 1, 0));
						cha.setMoveSpeed(1);
					} else { // スロー中
						int skillNum = 0;
						if (cha.hasSkillEffect(SKILL_SLOW)) {
							skillNum = SKILL_SLOW;
						} else if (cha.hasSkillEffect(SKILL_MASS_SLOW)) {
							skillNum = SKILL_MASS_SLOW;
						} else if (cha.hasSkillEffect(SKILL_ENTANGLE)) {
							skillNum = SKILL_ENTANGLE;
						}
						if (skillNum != 0) {
							cha.removeSkillEffect(skillNum);
							cha.removeSkillEffect(SKILL_HASTE);
							cha.setMoveSpeed(0);
							continue;
						}
					}
				} else if (_skillId == SKILL_CURE_POISON) {
					cha.curePoison();
				} else if (_skillId == SKILL_REMOVE_CURSE) {
//特殊狀態下狀態無法使用技能補血&聖光
					if (cha.hasSkillEffect(STATUS_CURSE_PARALYZED) //木乃尹
							||cha.hasSkillEffect(SKILL_STUN_SHOCK) //衝擊之暈
							||cha.hasSkillEffect(SKILL_FOG_OF_SLEEPING) //沉睡之霧
							||cha.hasSkillEffect(SKILL_ICE_LANCE)){ //冰矛圍籬
						_player.sendPackets(new S_ServerMessage(SystemMessageId.$285));
						return;
					}
					cha.curePoison();
					if (cha.hasSkillEffect(STATUS_CURSE_PARALYZING)
							|| cha.hasSkillEffect(STATUS_CURSE_PARALYZED)) {
						cha.cureParalaysis();
					}
				} else if (_skillId == SKILL_RESURRECTION
						|| _skillId == SKILL_GREATER_RESURRECTION) { // リザレクション、グレーターリザレクション
					if (cha instanceof L1PcInstance) {
						L1PcInstance pc = (L1PcInstance) cha;
						if (_player.getId() != pc.getId()) {
							if (L1World.getInstance().getVisiblePlayer(pc, 0).size() > 0) {
								for (L1PcInstance visiblePc : L1World.getInstance().getVisiblePlayer(pc, 0)) {
									if (!visiblePc.isDead()) {
										_player.sendPackets(new S_ServerMessage(SystemMessageId.$592));
										return;
									}
								}
							}
							if (pc.getCurrentHp() == 0 && pc.isDead()) {
								if (pc.getMap().isUseResurrection()) {
									if (_skillId == SKILL_RESURRECTION) {
										pc.setGres(false);
									} else if (_skillId == SKILL_GREATER_RESURRECTION) {
										pc.setGres(true);
									}
									pc.setTempID(_player.getId());
									pc.sendPackets(new S_Message_YN(SystemMessageId.$322, ""));
								}
							}
						}
					}
					if (cha instanceof L1NpcInstance) {
						if (!(cha instanceof L1TowerInstance)) {
							L1NpcInstance npc = (L1NpcInstance) cha;
							if (npc.getNpcTemplate().isCantResurrect() && !(npc instanceof L1PetInstance)) {
								return;
							}
							if (npc instanceof L1PetInstance && L1World.getInstance().getVisiblePlayer(npc, 0).size() > 0) {
								for (L1PcInstance visiblePc : L1World.getInstance().getVisiblePlayer(npc, 0)) {
									if (!visiblePc.isDead()) {
										_player.sendPackets(new S_ServerMessage(SystemMessageId.$592));
										return;
									}
								}
							}
							if (npc.getCurrentHp() == 0 && npc.isDead()) {
								npc.resurrect(npc.getMaxHp() / 4);
								npc.setResurrect(true);
							}
						}
					}
				} else if (_skillId == SKILL_NATURES_MIRACLE) { // コール オブ ネイチャー
					if (cha instanceof L1PcInstance) {
						L1PcInstance pc = (L1PcInstance) cha;
						if (_player.getId() != pc.getId()) {
							if (L1World.getInstance().getVisiblePlayer(pc, 0).size() > 0) {
								for (L1PcInstance visiblePc : L1World.getInstance().getVisiblePlayer(pc, 0)) {
									if (!visiblePc.isDead()) {
										_player.sendPackets(new S_ServerMessage(SystemMessageId.$592));
										return;
									}
								}
							}
							if (pc.getCurrentHp() == 0 && pc.isDead()) {
								pc.setTempID(_player.getId());
								pc.sendPackets(new S_Message_YN(SystemMessageId.$322, ""));
							}
						}
					}
					if (cha instanceof L1NpcInstance) {
						if (!(cha instanceof L1TowerInstance)) {
							L1NpcInstance npc = (L1NpcInstance) cha;
							if (npc.getNpcTemplate().isCantResurrect() && !(npc instanceof L1PetInstance)) {
								return;
							}
							if (npc instanceof L1PetInstance && L1World.getInstance().getVisiblePlayer(npc, 0).size() > 0) {
								for (L1PcInstance visiblePc : L1World.getInstance().getVisiblePlayer(npc, 0)) {
									if (!visiblePc.isDead()) {
										_player.sendPackets(new S_ServerMessage(SystemMessageId.$592));
										return;
									}
								}
							}
							if (npc.getCurrentHp() == 0 && npc.isDead()) {
								npc.resurrect(cha.getMaxHp());// HPを全回復する
								npc.resurrect(cha.getMaxMp() / 100);// MPを0にする
								npc.setResurrect(true);
							}
						}
					}
				} else if (_skillId == SKILL_DETECTION) { // ディテクション
					if (cha instanceof L1NpcInstance) {
						L1NpcInstance npc = (L1NpcInstance) cha;
						int hiddenStatus = npc.getHiddenStatus();
						if (hiddenStatus == L1NpcInstance.HIDDEN_STATUS_SINK) {
							npc.appearOnGround(_player);
						}
					}
				} else if (_skillId == SKILL_COUNTER_DETECTION) { // カウンターディテクション
					if (cha instanceof L1PcInstance) {
						dmg = _magic.calcMagicDamage(_skillId);
					} else if (cha instanceof L1NpcInstance) {
						L1NpcInstance npc = (L1NpcInstance) cha;
						int hiddenStatus = npc.getHiddenStatus();
						if (hiddenStatus == L1NpcInstance.HIDDEN_STATUS_SINK) {
							npc.appearOnGround(_player);
						} else {
							dmg = 0;
						}
					} else {
						dmg = 0;
					}
				} else if (_skillId == SKILL_TRUE_TARGET) { // トゥルーターゲット
					if (_user instanceof L1PcInstance) {
						L1PcInstance pri = (L1PcInstance) _user;
						pri.sendPackets(new S_TrueTarget(_targetID, pri.getId(), _message));
						L1PcInstance players[] = L1World.getInstance().getClan(pri.getClanname()).getOnlineClanMember();
						for (L1PcInstance pc : players) {
							pc.sendPackets(new S_TrueTarget(_targetID, pc.getId(), _message));
						}
					}
				} else if (_skillId == SKILL_ELEMENTAL_FALL_DOWN) { // エレメンタルフォールダウン
					if (_user instanceof L1PcInstance) {
						int playerAttr = _player.getElfAttr();
						int i = -50;
						if (cha instanceof L1PcInstance) {
							L1PcInstance pc = (L1PcInstance) cha;
							switch (playerAttr) {
								case 0:
									_player.sendPackets(new S_ServerMessage(SystemMessageId.$79));
								break;
								case 1:
									pc.addEarth(i);
									pc.setAddAttrKind(1);
								break;
								case 2:
									pc.addFire(i);
									pc.setAddAttrKind(2);
								break;
								case 4:
									pc.addWater(i);
									pc.setAddAttrKind(4);
								break;
								case 8:
									pc.addWind(i);
									pc.setAddAttrKind(8);
								break;
								default:
								break;
							}
						} else if (cha instanceof L1MonsterInstance) {
							L1MonsterInstance mob = (L1MonsterInstance) cha;
							switch (playerAttr) {
								case 0:
									_player.sendPackets(new S_ServerMessage(SystemMessageId.$79));
								break;
								case 1:
									mob.addEarth(i);
									mob.setAddAttrKind(1);
								break;
								case 2:
									mob.addFire(i);
									mob.setAddAttrKind(2);
								break;
								case 4:
									mob.addWater(i);
									mob.setAddAttrKind(4);
								break;
								case 8:
									mob.addWind(i);
									mob.setAddAttrKind(8);
								break;
								default:
								break;
							}
						}
					}
				}
				// ★★★ 回復系スキル ★★★
				else if ((_skillId == SKILL_LESSER_HEAL || _skillId == SKILL_HEAL
						|| _skillId == SKILL_GREATER_HEAL || _skillId == SKILL_FULL_HEAL
						|| _skillId == SKILL_HEAL_PLEDGE || _skillId == SKILL_NATURES_TOUCH
						|| _skillId == SKILL_NATURES_BLESSING)
						&& (_user instanceof L1PcInstance)) {
//特殊狀態中則無法施展回復系法術
					if (cha.hasSkillEffect(STATUS_CURSE_PARALYZED) //木乃伊
							||cha.hasSkillEffect(SKILL_STUN_SHOCK) //衝擊之暈
							||cha.hasSkillEffect(SKILL_FOG_OF_SLEEPING) //沉睡之霧
							||cha.hasSkillEffect(SKILL_ICE_LANCE)){ //冰矛圍籬
						_player.sendPackets(new S_ServerMessage(SystemMessageId.$285));
						return;
					} else if (_user instanceof L1PcInstance) {
						cha.removeSkillEffect(SKILL_WATER_LIFE);
					}
					//cha.removeSkillEffect(WATER_LIFE);
//add end
				}
				// ★★★ 攻擊系スキル ★★★
				// チルタッチ、バンパイアリックタッチ
				else if (_skillId == SKILL_CHILL_TOUCH || _skillId == SKILL_VAMPIRIC_TOUCH) {
					heal = dmg;
				} else if (_skillId == SKILL_TRIPLE_ARROW) { // トリプルアロー
					// 1回射出する每にアロー、ダメージ、命中を計算する
					// アローが殘り1でサイハの弓を持ってるとき、
					// 最初は普通の攻擊その後は魔法攻擊
					// アローが殘り1で普通の弓を持ってるとき，最初は普通の攻擊，
					// その後はアローの射出を行わず動きだけを行う。

					// GFX Check (Made by HuntBoy)
					boolean gfxcheck = false;
					int[] BowGFX = { 138, 37, 3860, 3126, 3420, 2284, 3105,
							3145, 3148, 3151, 3871, 4125, 2323, 3892, 3895,
							3898, 3901, 4917, 4918, 4919, 4950, 6087, 6140,
							6145, 6150, 6155, 6160, 6269, 6272, 6275, 6278,
							6826, 6827, 6836, 6837, 6846, 6847, 6856, 6857,
							6866, 6867, 6876, 6877, 6886, 6887 };
					int playerGFX = _player.getTempCharGfx();
					for (int gfx : BowGFX) {
						if (playerGFX == gfx) {
							gfxcheck = true;
							break;
						}
					}
					if (!gfxcheck) {
						return;
					}

					for (int i = 3; i > 0; i--) {
						_target.onAction(_player);
					}
					_player.sendPackets(new S_SkillSound(_player.getId(), 4394));
					_player.broadcastPacket(new S_SkillSound(_player.getId(), 4394));
				} else if (_skillId == SKILL_FOE_SLAYER) { // フォースレイヤー
					for (int i = 3; i > 0; i--) {
						_target.onAction(_player);
					}
					_player.sendPackets(new S_SkillSound(_target.getId(), 6509));
					_player.sendPackets(new S_SkillSound(_player.getId(), 7020));
					_player.broadcastPacket(new S_SkillSound(_target.getId(), 6509));
					_player.broadcastPacket(new S_SkillSound(_player.getId(), 7020));
				} else if (_skillId == 10026 || _skillId == 10027
						|| _skillId == 10028 || _skillId == 10029) { // 安息攻擊
					if (_user instanceof L1NpcInstance) {
						_user.broadcastPacket(new S_NpcChatPacket(_npc, (cha.getName())+("! ")+("$3717"), 0)); //龍的安息字串
					} else {
						_player.broadcastPacket(new S_ChatPacket(_player, (cha.getName())+("! ")+("$3717"), 0, 0)); //龍的安息字串
					}
				} else if (_skillId == 10057) { // 引き寄せ
					L1Teleport.teleportToTargetFront(cha, _user, 1);
				}

				// ★★★ 確率系スキル ★★★
				else if (_skillId == SKILL_SLOW || _skillId == SKILL_MASS_SLOW || _skillId == SKILL_ENTANGLE) { // スロー、マス
					// スロー、エンタングル
					if (cha instanceof L1PcInstance) {
						L1PcInstance pc = (L1PcInstance) cha;
						if (pc.getHasteItemEquipped() > 0) {
							continue;
						}
					}
					if (cha.getMoveSpeed() == 0) {
						if (cha instanceof L1PcInstance) {
							L1PcInstance pc = (L1PcInstance) cha;
							pc.sendPackets(new S_SkillHaste(pc.getId(), 2, _getBuffIconDuration));
						}
						cha.broadcastPacket(new S_SkillHaste(cha.getId(), 2, _getBuffIconDuration));
						cha.setMoveSpeed(2);
					} else if (cha.getMoveSpeed() == 1) {
						int skillNum = 0;
						if (cha.hasSkillEffect(SKILL_HASTE)) {
							skillNum = SKILL_HASTE;
						} else if (cha.hasSkillEffect(SKILL_GREATER_HASTE)) {
							skillNum = SKILL_GREATER_HASTE;
						} else if (cha.hasSkillEffect(STATUS_HASTE)) {
							skillNum = STATUS_HASTE;
						}
						if (skillNum != 0) {
							cha.removeSkillEffect(skillNum);
							cha.removeSkillEffect(_skillId);
							cha.setMoveSpeed(0);
							continue;
						}
					}
				} else if (_skillId == SKILL_CURSE_BLIND || _skillId == SKILL_DARKNESS) {
					if (cha instanceof L1PcInstance) {
						L1PcInstance pc = (L1PcInstance) cha;
						if (pc.hasSkillEffect(STATUS_FLOATING_EYE)) {
							pc.sendPackets(new S_CurseBlind(2));
						} else {
							pc.sendPackets(new S_CurseBlind(1));
						}
					}
				} else if (_skillId == SKILL_CURSE_POISON) {
					L1DamagePoison.doInfection(_user, cha, 3000, 5);
				} else if (_skillId == SKILL_CURSE_PARALYZE
						|| _skillId == CURSE_PARALYZE2) {
					if (!cha.hasSkillEffect(SKILL_EARTH_BIND)
							&& !cha.hasSkillEffect(SKILL_ICE_LANCE)
							&& !cha.hasSkillEffect(SKILL_FREEZING_BLIZZARD)
							&& !cha.hasSkillEffect(SKILL_FREEZING_BREATH)) {
						if (cha instanceof L1PcInstance) {
							L1CurseParalysis.curse(cha, 8000, 16000);
						} else if (cha instanceof L1MonsterInstance) {
							L1CurseParalysis.curse(cha, 0, 16000);
						}
					}
				} else if (_skillId == SKILL_WEAKNESS) { // ウィークネス
					if (cha instanceof L1PcInstance) {
						L1PcInstance pc = (L1PcInstance) cha;
						pc.addDmgup(-5);
						pc.addHitup(-1);
					}
				} else if (_skillId == SKILL_DISEASE) { // ディジーズ
					if (cha instanceof L1PcInstance) {
						L1PcInstance pc = (L1PcInstance) cha;
						pc.addDmgup(-6);
						pc.addAc(12);
					}
				} else if (_skillId == SKILL_ICE_LANCE // 冰矛圍籬 Start
						|| _skillId == SKILL_FREEZING_BLIZZARD) { // 冰雪颶風 Start
					_isFreeze = _magic.calcProbabilityMagic(_skillId);
					if (_isFreeze) {
						int time = _skill.getBuffDuration() * 1000;
						L1EffectSpawn.getInstance().spawnEffect(81168, time, cha.getX(), cha.getY(), cha.getMapId());
						if (cha instanceof L1PcInstance) {
							L1PcInstance pc = (L1PcInstance) cha;

							if(_skillId == SKILL_ICE_LANCE)
								pc.addInvincibleEffect(TRANSFORM_SKILL_ICE_LANCE);
							else
								pc.addInvincibleEffect(TRANSFORM_SKILL_FREEZING_BLIZZARD);

							pc.sendPackets(new S_Poison(pc.getId(), 2));
							pc.broadcastPacket(new S_Poison(pc.getId(), 2));
							pc.sendPackets(new S_Paralysis(S_Paralysis.TYPE_FREEZE, true));
						} else if (cha instanceof L1MonsterInstance
								|| cha instanceof L1SummonInstance
								|| cha instanceof L1PetInstance) {
							L1NpcInstance npc = (L1NpcInstance) cha;

							if(_skillId == SKILL_ICE_LANCE)
								npc.addInvincibleEffect(TRANSFORM_SKILL_ICE_LANCE);
							else
								npc.addInvincibleEffect(TRANSFORM_SKILL_FREEZING_BLIZZARD);

							npc.broadcastPacket(new S_Poison(npc.getId(), 2));
							npc.setParalyzed(true);
							npc.setParalysisTime(time);
						}
					}
				} else if (_skillId == SKILL_EARTH_BIND) { // 大地屏障 Start
					if (cha instanceof L1PcInstance) {
						L1PcInstance pc = (L1PcInstance) cha;
						pc.addInvincibleEffect(TRANSFORM_SKILL_EARTH_BIND);
						pc.sendPackets(new S_Poison(pc.getId(), 2));
						pc.broadcastPacket(new S_Poison(pc.getId(), 2));
						pc.sendPackets(new S_Paralysis(S_Paralysis.TYPE_FREEZE, true));
					} else if (cha instanceof L1MonsterInstance
							|| cha instanceof L1SummonInstance
							|| cha instanceof L1PetInstance) {
						L1NpcInstance npc = (L1NpcInstance) cha;
						npc.addInvincibleEffect(TRANSFORM_SKILL_EARTH_BIND);
						npc.broadcastPacket(new S_Poison(npc.getId(), 2));
						npc.setParalyzed(true);
						npc.setParalysisTime(_skill.getBuffDuration() * 1000);
					}
				} else if (_skillId == SKILL_STUN_SHOCK) {
					int[] stunTimeArray = { 1000, 2000, 3000, 4000, 5000, 6000 }; //waja chang 衝擊之暈時間為1-6秒
					int rnd = RandomArrayList.getInt(6); // 依存 stunTimeArray[] 大小
					_shockStunDuration = stunTimeArray[rnd];
					if (cha instanceof L1PcInstance && cha.hasSkillEffect(SKILL_STUN_SHOCK)) {
						_shockStunDuration += cha.getSkillEffectTimeSec(SKILL_STUN_SHOCK) * 1000;
					}

					L1EffectSpawn.getInstance().spawnEffect(81162, _shockStunDuration, cha.getX(), cha.getY(), cha.getMapId());
					if (cha instanceof L1PcInstance) {
						L1PcInstance pc = (L1PcInstance) cha;
						pc.sendPackets(new S_Paralysis(S_Paralysis.TYPE_STUN, true));
					} else if (cha instanceof L1MonsterInstance
							|| cha instanceof L1SummonInstance
							|| cha instanceof L1PetInstance) {
						L1NpcInstance npc = (L1NpcInstance) cha;
						npc.setParalyzed(true);
						npc.setParalysisTime(_shockStunDuration);
					}
				} else if (_skillId == SKILL_WIND_SHACKLE) { // ウィンド シャックル
					if (cha instanceof L1PcInstance) {
						L1PcInstance pc = (L1PcInstance) cha;
						pc.sendPackets(new S_SkillIconWindShackle(pc.getId(), _getBuffIconDuration));
					}
				} else if (_skillId == SKILL_CANCEL_MAGIC) {
					if (cha instanceof L1NpcInstance) {
						L1NpcInstance npc = (L1NpcInstance) cha;
						int npcId = npc.getNpcTemplate().get_npcId();
						if (npcId == 71092) { // 調查員
							if (npc.getGfxId() == npc.getTempCharGfx()) {
								npc.setTempCharGfx(1314);
								npc.broadcastPacket(new S_ChangeShape(npc.getId(), 1314));
								return;
							} else {
								return;
							}
						}
						if (npcId == 45640) { // ユニコーン
							if (npc.getGfxId() == npc.getTempCharGfx()) {
								npc.setCurrentHp(npc.getMaxHp());
								npc.setTempCharGfx(2332);
								npc.broadcastPacket(new S_ChangeShape(npc.getId(), 2332));
								npc.setName("$2103");
								npc.setNameId("$2103");
								npc.broadcastPacket(new S_ChangeName(npc.getId(), "$2103"));
							} else if (npc.getTempCharGfx() == 2332) {
								npc.setCurrentHp(npc.getMaxHp());
								npc.setTempCharGfx(2755);
								npc.broadcastPacket(new S_ChangeShape(npc.getId(), 2755));
								npc.setName("$2488");
								npc.setNameId("$2488");
								npc.broadcastPacket(new S_ChangeName(npc.getId(), "$2488"));
							}
						}
						if (npcId == 81209) { // ロイ
							if (npc.getGfxId() == npc.getTempCharGfx()) {
								npc.setTempCharGfx(4310);
								npc.broadcastPacket(new S_ChangeShape(npc.getId(), 4310));
								return;
							} else {
								return;
							}
						}
					}
					if (_player != null && _player.isInvisble()) {
						_player.delInvis();
					}
					if (!(cha instanceof L1PcInstance)) {
						L1NpcInstance npc = (L1NpcInstance) cha;
						npc.setMoveSpeed(0);
						npc.setBraveSpeed(0);
						npc.broadcastPacket(new S_SkillHaste(cha.getId(), 0, 0));
						npc.broadcastPacket(new S_SkillBrave(cha.getId(), 0, 0));
						npc.setWeaponBreaked(false);
						npc.setParalyzed(false);
						npc.setParalysisTime(0);
					}

					// スキルの解除
					for (int skillNum = SKILL_BEGIN; skillNum <= SKILL_END; skillNum++) {
						if (isNotCancelable(skillNum) && !cha.isDead()) {
							continue;
						}
						cha.removeSkillEffect(skillNum);
					}

					// ステータス強化、異常の解除
					cha.curePoison();
					cha.cureParalaysis();
					for (int skillNum = STATUS_BEGIN; skillNum <= STATUS_END; skillNum++) {
						if (skillNum == STATUS_CHAT_PROHIBITED // チャット禁止は解除しない
								|| skillNum == STATUS_CURSE_BARLOG // バルログの呪いは解除しない
								|| skillNum == STATUS_CURSE_YAHEE) { // ヤヒの呪いは解除しない
							continue;
						}
						cha.removeSkillEffect(skillNum);
					}

					if (cha instanceof L1PcInstance) {
					}

					// 料理の解除
					for (int skillNum = COOKING_BEGIN; skillNum <= COOKING_END; skillNum++) {
						if (isNotCancelable(skillNum)) {
							continue;
						}
						cha.removeSkillEffect(skillNum);
					}

					if (cha instanceof L1PcInstance) {
						L1PcInstance pc = (L1PcInstance) cha;

						// アイテム装備による変身の解除
						L1PolyMorph.undoPoly(pc);
						pc.sendPackets(new S_CharVisualUpdate(pc));
						pc.broadcastPacket(new S_CharVisualUpdate(pc));

						// ヘイストアイテム装備時はヘイスト関連のスキルが何も掛かっていないはずなのでここで解除
						if (pc.getHasteItemEquipped() > 0) {
							pc.setMoveSpeed(0);
							pc.sendPackets(new S_SkillHaste(pc.getId(), 0, 0));
							pc.broadcastPacket(new S_SkillHaste(pc.getId(), 0, 0));
						}
					}
					cha.removeSkillEffect(STATUS_FREEZE); // Freeze解除
					if (cha instanceof L1PcInstance) {
						L1PcInstance pc = (L1PcInstance) cha;
						pc.sendPackets(new S_CharVisualUpdate(pc));
						pc.broadcastPacket(new S_CharVisualUpdate(pc));
						if (pc.isPrivateShop()) {
							pc.sendPackets(new S_DoActionShop(pc.getId(), ActionCodes.ACTION_Shop, pc.getShopChat()));
							pc.broadcastPacket(new S_DoActionShop(pc.getId(), ActionCodes.ACTION_Shop, pc.getShopChat()));
						}
						if (_user instanceof L1PcInstance) {
							L1PinkName.onAction(pc, _user);
						}
					}
				} else if (_skillId == SKILL_TURN_UNDEAD // ターン アンデッド
						&& (undeadType == 1 || undeadType == 3)) {
					// ダメージを對象のHPとする。
					dmg = cha.getCurrentHp();
				} else if (_skillId == SKILL_MANA_DRAIN) { // マナ ドレイン
					int chance = RandomArrayList.getInc(10, 5);
					drainMana = chance + (_user.getInt() / 2);
					if (cha.getCurrentMp() < drainMana) {
						drainMana = cha.getCurrentMp();
					}
				} else if (_skillId == SKILL_WEAPON_BREAK) { // ウェポン ブレイク
					/*
					 * 對NPCの場合、L1Magicのダメージ算出でダメージ1/2としているので
					 * こちらには、對PCの場合しか記入しない。 損傷量は1~(int/3)まで
					 */
					if (_targetType == PC_PC || _targetType == NPC_PC) {
						if (cha instanceof L1PcInstance) {
							L1PcInstance pc = (L1PcInstance) cha;
							L1ItemInstance weapon = pc.getWeapon();
							if (weapon != null) {
								int weaponDamage = RandomArrayList.getInc(_user.getInt() / 3, 1);
								// \f1あなたの%0が損傷しました。
								pc.sendPackets(new S_ServerMessage(SystemMessageId.$268, weapon.getLogName()));
								pc.getInventory().receiveDamage(weapon, weaponDamage);
							}
						}
					} else {
						((L1NpcInstance) cha).setWeaponBreaked(true);
					}
				} else if (_skillId == SKILL_FOG_OF_SLEEPING) {
					if (cha instanceof L1PcInstance) {
						L1PcInstance pc = (L1PcInstance) cha;
						pc.sendPackets(new S_Paralysis(S_Paralysis.TYPE_SLEEP, true));
					}
					cha.setSleeped(true);
				} else if (_skillId == STATUS_FREEZE) { // Freeze
					if (cha instanceof L1PcInstance) {
						L1PcInstance pc = (L1PcInstance) cha;
						pc.sendPackets(new S_Paralysis(S_Paralysis.TYPE_BIND, true));
					}
				} else if (_skillId == SKILL_GUARD_BRAKE) { // ガードブレイク
					if (cha instanceof L1PcInstance) {
						L1PcInstance pc = (L1PcInstance) cha;
						pc.addAc(15);
					}
				} else if (_skillId == SKILL_HORROR_OF_DEATH) { // ホラーオブデス
					if (cha instanceof L1PcInstance) {
						L1PcInstance pc = (L1PcInstance) cha;
						pc.addStr(-5);
						pc.addInt(-5);
					}
				}

				// ●●●● PCにしか效果のないスキル ●●●●
				if (_targetType == PC_PC || _targetType == NPC_PC) {
					// ★★★ 特殊系スキル★★★
					if (_skillId == SKILL_TELEPORT || _skillId == SKILL_MASS_TELEPORT) { // マステレ、テレポート
						L1PcInstance pc = (L1PcInstance) cha;
						L1BookMark bookm = pc.getBookMark(_bookmarkId);
						if (bookm != null) { // ブックマークを取得出來たらテレポート
							if (pc.getMap().isEscapable() || pc.isGm()) {
								int newX = bookm.getLocX();
								int newY = bookm.getLocY();
								short mapId = bookm.getMapId();

								if (_skillId == SKILL_MASS_TELEPORT) { // マステレポート
									List<L1PcInstance> clanMember = L1World.getInstance().getVisiblePlayer(pc);
									for (L1PcInstance member : clanMember) {
										if (pc.getLocation().getTileLineDistance(member.getLocation()) <= 3 &&
												member.getClanid() == pc.getClanid() && pc.getClanid() != 0 &&
												member.getId() != pc.getId()) {
											L1Teleport.teleport(member, newX, newY, mapId, 5, true);
										}
									}
								}
								L1Teleport.teleport(pc, newX, newY, mapId, 5, true);
							} else { // テレポート不可マップへの移動制限
								L1Teleport.teleport(pc, pc.getX(), pc.getY(), pc.getMapId(), pc.getHeading(), false);
								pc.sendPackets(new S_ServerMessage(SystemMessageId.$79));
							}
						} else { // ブックマークが取得出來なかった、あるいは「任意の場所」を選擇した場合の處理
							if (pc.getMap().isTeleportable() || pc.isGm()) {
								L1Location newLocation = pc.getLocation().randomLocation(200, true);
								int newX = newLocation.getX();
								int newY = newLocation.getY();
								short mapId = (short) newLocation.getMapId();

								if (_skillId == SKILL_MASS_TELEPORT) { // マステレポート
									List<L1PcInstance> clanMember = L1World.getInstance().getVisiblePlayer(pc);
									for (L1PcInstance member : clanMember) {
										if (pc.getLocation().getTileLineDistance(member.getLocation()) <= 3
												&& member.getClanid() == pc.getClanid()
												&& pc.getClanid() != 0 && member.getId() != pc.getId()) {
											L1Teleport.teleport(member, newX, newY, mapId, 5, true);
										}
									}
								}
								L1Teleport.teleport(pc, newX, newY, mapId, 5, true);
							} else {
								pc.sendPackets(new S_ServerMessage(SystemMessageId.$276));
								L1Teleport.teleport(pc, pc.getX(), pc.getY(), pc.getMapId(), pc.getHeading(), false);
							}
						}
					} else if (_skillId == SKILL_TELEPORT_TO_MATHER) { // テレポート トゥ
						// マザー
						L1PcInstance pc = (L1PcInstance) cha;
						if (pc.getMap().isEscapable() || pc.isGm()) {
							L1Teleport.teleport(pc, 33051, 32337, (short) 4, 5, true);
						} else {
							pc.sendPackets(new S_ServerMessage(SystemMessageId.$647));
							L1Teleport.teleport(pc, pc.getX(), pc.getY(), pc.getMapId(), pc.getHeading(), false);
						}
					} else if (_skillId == SKILL_CALL_PLEDGE_MEMBER) { // コールクラン
						L1PcInstance pc = (L1PcInstance) cha;
						L1PcInstance clanPc = (L1PcInstance) L1World.getInstance().findObject(_targetID);
						if (clanPc != null) {
							clanPc.setTempID(pc.getId()); // 相手のオブジェクトIDを保存しておく
							clanPc.sendPackets(new S_Message_YN(SystemMessageId.$729, ""));
						}
					} else if (_skillId == SKILL_RUN_CLAN) { // ランクラン
						L1PcInstance pc = (L1PcInstance) cha;
						L1PcInstance clanPc = (L1PcInstance) L1World
								.getInstance().findObject(_targetID);
						if (clanPc != null) {
							if (pc.getMap().isEscapable() || pc.isGm()) {
								boolean castle_area = L1CastleLocation.checkInAllWarArea(
								// いずれかの城エリア
								clanPc.getX(), clanPc.getY(), clanPc.getMapId());
								if ((clanPc.getMapId() == 0
										|| clanPc.getMapId() == 4 || clanPc
										.getMapId() == 304)
										&& castle_area == false) {
									L1Teleport.teleport(pc, clanPc.getX(), clanPc.getY(), clanPc.getMapId(), 5, true);
								} else {
									//pc.sendPackets(new S_ServerMessage(547));
									pc.sendPackets(new S_ServerMessage(SystemMessageId.$166, "你的盟友在你無法傳送前往的地區"));
								}
							} else {
								pc.sendPackets(new S_ServerMessage(SystemMessageId.$647));
								L1Teleport.teleport(pc, pc.getX(), pc.getY(), pc.getMapId(), pc.getHeading(), false);
							}
						}
					} else if (_skillId == SKILL_CREATE_MAGICAL_WEAPON) { // クリエイト
						// マジカル ウェポン
						L1PcInstance pc = (L1PcInstance) cha;
						L1ItemInstance item = pc.getInventory().getItem(_itemobjid);
						if (item != null && item.getItem().getType2() == 1) {
							int item_type = item.getItem().getType2();
							int safe_enchant = item.getItem().get_safeenchant();
							int enchant_level = item.getEnchantLevel();
							String item_name = item.getName();
							if (safe_enchant < 0) { // 強化不可
								pc.sendPackets(new S_ServerMessage(SystemMessageId.$79));
							} else if (safe_enchant == 0) { // 安全圈+0
								pc.sendPackets(new S_ServerMessage(SystemMessageId.$79));
							} else if (item_type == 1 && enchant_level == 0) {
								if (!item.isIdentified()) {// 未鑑定
									pc.sendPackets(new S_ServerMessage(SystemMessageId.$161, item_name, "$245", "$247"));
								} else {
									item_name = "+0 " + item_name;
									pc.sendPackets(new S_ServerMessage(SystemMessageId.$161, "+0 " + item_name, "$245", "$247"));
								}
								item.setEnchantLevel(1);
								pc.getInventory().updateItem(item, L1PcInventory.COL_ENCHANTLVL);
							} else {
								pc.sendPackets(new S_ServerMessage(SystemMessageId.$79));
							}
						} else {
							pc.sendPackets(new S_ServerMessage(SystemMessageId.$79));
						}
					} else if (_skillId == SKILL_PURIFY_STONE) { // ブリング ストーン
						L1PcInstance pc = (L1PcInstance) cha;
						L1ItemInstance item = pc.getInventory().getItem(_itemobjid);
						if (item != null) {
							int dark = (int) (10 + (pc.getLevel() * 0.8) + (pc.getWis() - 6) * 1.2);
							int brave = (int) (dark / 2.1);
							int wise = (int) (brave / 2.0);
							int kayser = (int) (wise / 1.9);
							int chance = RandomArrayList.getInc(100, 1);
							if (item.getItem().getItemId() == 40320) {
								pc.getInventory().removeItem(item, 1);
								if (dark >= chance) {
									pc.getInventory().storeItem(40321, 1);
									pc.sendPackets(new S_ServerMessage(SystemMessageId.$403, "$2475"));
								} else {
									pc.sendPackets(new S_ServerMessage(SystemMessageId.$280));
								}
							} else if (item.getItem().getItemId() == 40321) {
								pc.getInventory().removeItem(item, 1);
								if (brave >= chance) {
									pc.getInventory().storeItem(40322, 1);
									pc.sendPackets(new S_ServerMessage(SystemMessageId.$403, "$2476"));
								} else {
									pc.sendPackets(new S_ServerMessage(SystemMessageId.$280));
								}
							} else if (item.getItem().getItemId() == 40322) {
								pc.getInventory().removeItem(item, 1);
								if (wise >= chance) {
									pc.getInventory().storeItem(40323, 1);
									pc.sendPackets(new S_ServerMessage(SystemMessageId.$403, "$2477"));
								} else {
									pc.sendPackets(new S_ServerMessage(SystemMessageId.$280));
								}
							} else if (item.getItem().getItemId() == 40323) {
								pc.getInventory().removeItem(item, 1);
								if (kayser >= chance) {
									pc.getInventory().storeItem(40324, 1);
									pc.sendPackets(new S_ServerMessage(SystemMessageId.$403, "$2478"));
								} else {
									pc.sendPackets(new S_ServerMessage(SystemMessageId.$280));
								}
							}
						}
					} else if (_skillId == SKILL_SUMMON_MONSTER) { // サモンモンスター
						L1PcInstance pc = (L1PcInstance) cha;
						int level = pc.getLevel();
						int[] summons;
						if (pc.getMap().isRecallPets() || pc.isGm()) {
							if (pc.getInventory().checkEquipped(20284)) {
								pc.sendPackets(new S_ShowSummonList(pc.getId()));
								if (!pc.isSummonMonster()) {//判斷是否無道具施法(召戒清單、變身清單)
									pc.setSummonMonster(true);
								}
							} else {
/*
 * summons = new int[] { 81083, 81084, 81085, 81086, 81087, 81088, 81089 };
 */
								summons = new int[] { 81210, 81213, 81216,
										81219, 81222, 81225, 81228 };
								int summonid = 0;
//								int summoncost = 6;
								int summoncost = 8;
								int levelRange = 32;
								for (int i = 0; i < summons.length; i++) { // 該當ＬＶ範圍檢索
									if (level < levelRange
											|| i == summons.length - 1) {
										summonid = summons[i];
										break;
									}
									levelRange += 4;
								}

								int petcost = 0;
								Object[] petlist = pc.getPetList().values()
										.toArray();
								for (Object pet : petlist) {
									// 現在のペットコスト
									petcost += ((L1NpcInstance) pet)
											.getPetcost();
								}
								int pcCha = pc.getCha();
								if (pcCha > 34) { // max count = 5
									pcCha = 34;
								}
								int charisma = pcCha + 6 - petcost; 
// int charisma = pc.getCha() + 6 - petcost;
								int summoncount = charisma / summoncost;
								L1Npc npcTemp = NpcTable.getInstance()
										.getTemplate(summonid);
								for (int i = 0; i < summoncount; i++) {
									L1SummonInstance summon = new L1SummonInstance(
											npcTemp, pc);
									summon.setPetcost(summoncost);
								}
							}
						} else {
							pc.sendPackets(new S_ServerMessage(SystemMessageId.$79));
						}
					} else if (_skillId == SKILL_SUMMON_LESSER_ELEMENTAL
							|| _skillId == SKILL_SUMMON_GREATER_ELEMENTAL) { // レッサーエレメンタル、グレーターエレメンタル
						L1PcInstance pc = (L1PcInstance) cha;
						int attr = pc.getElfAttr();
						if (attr != 0) { // 無屬性でなければ實行
							if (pc.getMap().isRecallPets() || pc.isGm()) {
								int petcost = 0;
								Object[] petlist = pc.getPetList().values()
										.toArray();
								for (Object pet : petlist) {
									// 現在のペットコスト
									petcost += ((L1NpcInstance) pet)
											.getPetcost();
								}

								if (petcost == 0) { // 1匹も所屬NPCがいなければ實行
									int summonid = 0;
									int summons[];
									if (_skillId == SKILL_SUMMON_LESSER_ELEMENTAL) { // レッサーエレメンタル[地,火,水,風]
										summons = new int[] { 45306, 45303,
												45304, 45305 };
									} else {
										// グレーターエレメンタル[地,火,水,風]
										summons = new int[] { 81053, 81050,
												81051, 81052 };
									}
									int npcattr = 1;
									for (int i = 0; i < summons.length; i++) {
										if (npcattr == attr) {
											summonid = summons[i];
											i = summons.length;
										}
										npcattr *= 2;
									}
									// 特殊設定の場合ランダムで出現
									if (summonid == 0) {
										int k3 = RandomArrayList.getInt(4);
										summonid = summons[k3];
									}

									L1Npc npcTemp = NpcTable.getInstance()
											.getTemplate(summonid);
									L1SummonInstance summon = new L1SummonInstance(
											npcTemp, pc);
									summon.setPetcost(pc.getCha() + 7); // 精靈の他にはNPCを所屬させられない
								}
							} else {
								pc.sendPackets(new S_ServerMessage(SystemMessageId.$79));
							}
						}
					} else if (_skillId == SKILL_ABSOLUTE_BARRIER) { // 絕對屏障 Start
						L1PcInstance pc = (L1PcInstance) cha;
						pc.addInvincibleEffect(TRANSFORM_SKILL_ABSOLUTE_BARRIER);
						pc.stopHpRegeneration();
						pc.stopHpRegenerationByDoll();
						pc.stopMpRegeneration();
						pc.stopMpRegenerationByDoll();
					}

					// ★★★ 變化系スキル（エンチャント） ★★★
					if (_skillId == SKILL_LIGHT) { // ライト
						// addMagicList()後に、turnOnOffLight()でパケット送信
					} else if (_skillId == SKILL_GLOWING_AURA) { // グローウィング オーラ
						L1PcInstance pc = (L1PcInstance) cha;
						pc.addHitup(5);
						pc.addBowHitup(5);
						pc.addMr(20);
						pc.sendPackets(new S_SPMR(pc));
						pc.sendPackets(new S_SkillIconAura(113, _getBuffIconDuration));
					} else if (_skillId == SKILL_SHINING_AURA) { // シャイニング オーラ
						L1PcInstance pc = (L1PcInstance) cha;
						pc.addAc(-8);
						pc.sendPackets(new S_SkillIconAura(114, _getBuffIconDuration));
					} else if (_skillId == SKILL_BRAVE_AURA) { // ブレイブ オーラ
						L1PcInstance pc = (L1PcInstance) cha;
						pc.addDmgup(5);
						pc.sendPackets(new S_SkillIconAura(116, _getBuffIconDuration));
					} else if (_skillId == SKILL_SHIELD) { // シールド
						L1PcInstance pc = (L1PcInstance) cha;
						pc.addAc(-2);
						pc.sendPackets(new S_SkillIconShield(5, _getBuffIconDuration));
					} else if (_skillId == SKILL_SHADOW_ARMOR) { // シャドウ アーマー
						L1PcInstance pc = (L1PcInstance) cha;
						pc.addAc(-3);
						pc.sendPackets(new S_SkillIconShield(3, _getBuffIconDuration));
					} else if (_skillId == SKILL_DRESS_DEXTERITY) { // ドレス デクスタリティー
						L1PcInstance pc = (L1PcInstance) cha;
						pc.addDex(2);
						pc.sendPackets(new S_DexUp(pc, 2, _getBuffIconDuration));
					} else if (_skillId == SKILL_DRESS_MIGHTY) { // ドレス マイティー
						L1PcInstance pc = (L1PcInstance) cha;
						pc.addStr(2);
						pc.sendPackets(new S_StrUp(pc, 2, _getBuffIconDuration));
					} else if (_skillId == SKILL_SHADOW_FANG) { // シャドウ ファング
						L1PcInstance pc = (L1PcInstance) cha;
						L1ItemInstance item = pc.getInventory().getItem(_itemobjid);
						if (item != null && item.getItem().getType2() == 1) {
							item.setSkillWeaponEnchant(pc, _skillId, _skill.getBuffDuration() * 1000);
						} else {
							pc.sendPackets(new S_ServerMessage(SystemMessageId.$79));
						}
					} else if (_skillId == SKILL_ENCHANT_WEAPON) { // エンチャント ウェポン
						L1PcInstance pc = (L1PcInstance) cha;
						L1ItemInstance item = pc.getInventory().getItem(_itemobjid);
						if (item != null && item.getItem().getType2() == 1) {
							pc.sendPackets(new S_ServerMessage(SystemMessageId.$161, item.getLogName(), "$245", "$247"));
							item.setSkillWeaponEnchant(pc, _skillId, _skill.getBuffDuration() * 1000);
						} else {
							pc.sendPackets(new S_ServerMessage(SystemMessageId.$79));
						}
					} else if (_skillId == SKILL_HOLY_WEAPON // ホーリー ウェポン
							|| _skillId == SKILL_BLESS_WEAPON) { // ブレス ウェポン
						if (!(cha instanceof L1PcInstance)) {
							return;
						}
						L1PcInstance pc = (L1PcInstance) cha;
						if (pc.getWeapon() == null) {
							pc.sendPackets(new S_ServerMessage(SystemMessageId.$79));
							return;
						}
						for (L1ItemInstance item : pc.getInventory().getItems()) {
							if (pc.getWeapon().equals(item)) {
								pc.sendPackets(new S_ServerMessage(SystemMessageId.$161, item.getLogName(), "$245", "$247"));
								item.setSkillWeaponEnchant(pc, _skillId, _skill.getBuffDuration() * 1000);
								return;
							}
						}
					} else if (_skillId == SKILL_BLESSED_ARMOR) { // ブレスド アーマー
						L1PcInstance pc = (L1PcInstance) cha;
						L1ItemInstance item = pc.getInventory()
								.getItem(_itemobjid);
						if (item != null && item.getItem().getType2() == 2
								&& item.getItem().getType() == 2) {
							pc.sendPackets(new S_ServerMessage(SystemMessageId.$161, item.getLogName(), "$245", "$247"));
							item.setSkillArmorEnchant(pc, _skillId, _skill.getBuffDuration() * 1000);
						} else {
							pc.sendPackets(new S_ServerMessage(SystemMessageId.$79));
						}
					} else if (_skillId == SKILL_BLESS_OF_EARTH) { // アース ブレス
						L1PcInstance pc = (L1PcInstance) cha;
						pc.addAc(-7);
						pc.sendPackets(new S_SkillIconShield(7, _getBuffIconDuration));
					} else if (_skillId == SKILL_RESIST_MAGIC) { // レジスト マジック
						L1PcInstance pc = (L1PcInstance) cha;
						pc.addMr(10);
						pc.sendPackets(new S_SPMR(pc));
					} else if (_skillId == SKILL_CLEAR_MIND) { // クリアー マインド
						L1PcInstance pc = (L1PcInstance) cha;
						pc.addWis(3);
						pc.resetBaseMr();
					} else if (_skillId == SKILL_RESIST_ELEMENTAL) { // レジスト エレメント
						L1PcInstance pc = (L1PcInstance) cha;
						pc.addWind(10);
						pc.addWater(10);
						pc.addFire(10);
						pc.addEarth(10);
						pc.sendPackets(new S_OwnCharAttrDef(pc));
					} else if (_skillId == SKILL_BODY_TO_MIND) { // ボディ トゥ マインド
						L1PcInstance pc = (L1PcInstance) cha;
						pc.setCurrentMp(pc.getCurrentMp() + 2);
					} else if (_skillId == SKILL_BLOOD_TO_SOUL) { // ブラッディ ソウル
						L1PcInstance pc = (L1PcInstance) cha;
						pc.setCurrentMp(pc.getCurrentMp() + 12);
					} else if (_skillId == SKILL_PROTECTION_FROM_ELEMENTAL) { // エレメンタルプロテクション
						L1PcInstance pc = (L1PcInstance) cha;
						int attr = pc.getElfAttr();
						if (attr == 1) {
							pc.addEarth(50);
						} else if (attr == 2) {
							pc.addFire(50);
						} else if (attr == 4) {
							pc.addWater(50);
						} else if (attr == 8) {
							pc.addWind(50);
						}
					} else if (_skillId == SKILL_INVISIBILITY
							|| _skillId == SKILL_BLIND_HIDING) { // インビジビリティ、ブラインドハイディング
						L1PcInstance pc = (L1PcInstance) cha;
						pc.sendPackets(new S_Invis(pc.getId(), 1));
						pc.broadcastPacketForFindInvis(new S_RemoveObject(pc), false);
// pc.broadcastPacket(new S_RemoveObject(pc));
					} else if (_skillId == SKILL_IRON_SKIN) { // アイアン スキン
						L1PcInstance pc = (L1PcInstance) cha;
						pc.addAc(-10);
						pc.sendPackets(new S_SkillIconShield(10, _getBuffIconDuration));
					} else if (_skillId == SKILL_EARTH_SKIN) { // アース スキン
						L1PcInstance pc = (L1PcInstance) cha;
						pc.addAc(-6);
						pc.sendPackets(new S_SkillIconShield(6, _getBuffIconDuration));
					} else if (_skillId == SKILL_ENCHANT_MIGHTY) { // フィジカルエンチャント：STR
						L1PcInstance pc = (L1PcInstance) cha;
						pc.addStr(5);
						pc.sendPackets(new S_StrUp(pc, 5, _getBuffIconDuration));
					} else if (_skillId == SKILL_ENCHANT_DEXTERITY) { // フィジカルエンチャント：DEX
						L1PcInstance pc = (L1PcInstance) cha;
						pc.addDex(5);
						pc.sendPackets(new S_DexUp(pc, 5, _getBuffIconDuration));
					} else if (_skillId == SKILL_FIRE_WEAPON) { // ファイアー ウェポン
						L1PcInstance pc = (L1PcInstance) cha;
						pc.addDmgup(4);
						pc.sendPackets(new S_SkillIconAura(147, _getBuffIconDuration));
					} else if (_skillId == SKILL_BLESS_OF_FIRE) { // ファイアー ブレス
						L1PcInstance pc = (L1PcInstance) cha;
						pc.addDmgup(4);
						pc.sendPackets(new S_SkillIconAura(154, _getBuffIconDuration));
					} else if (_skillId == SKILL_BURNING_WEAPON) { // バーニング ウェポン
						L1PcInstance pc = (L1PcInstance) cha;
						pc.addDmgup(6);
						pc.addHitup(3);
						pc.sendPackets(new S_SkillIconAura(162, _getBuffIconDuration));
					} else if (_skillId == SKILL_WIND_SHOT) { // ウィンド ショット
						L1PcInstance pc = (L1PcInstance) cha;
						pc.addBowHitup(6);
						pc.sendPackets(new S_SkillIconAura(148, _getBuffIconDuration));
					} else if (_skillId == SKILL_EYE_OF_STORM) { // ストーム アイ
						L1PcInstance pc = (L1PcInstance) cha;
						pc.addBowHitup(2);
						pc.addBowDmgup(3);
						pc.sendPackets(new S_SkillIconAura(155, _getBuffIconDuration));
					} else if (_skillId == SKILL_STORM_SHOT) { // ストーム ショット
						L1PcInstance pc = (L1PcInstance) cha;
						pc.addBowDmgup(5);
						pc.addBowHitup(-1);
						pc.sendPackets(new S_SkillIconAura(165, _getBuffIconDuration));
					} else if (_skillId == SKILL_BERSERKERS) { // バーサーカー
						L1PcInstance pc = (L1PcInstance) cha;
						pc.addAc(10);
						pc.addDmgup(5);
						pc.addHitup(2);
					} else if (_skillId == SKILL_POLYMORPH) { // シェイプ チェンジ
						L1PcInstance pc = (L1PcInstance) cha;
						pc.sendPackets(new S_ShowPolyList(pc.getId()));
						if (!pc.isShapeChange()) { //判斷是否無道具施法(召戒清單、變身清單)
							pc.setShapeChange(true);
						}
					} else if (_skillId == SKILL_ADVANCE_SPIRIT) { // アドバンスド スピリッツ
						L1PcInstance pc = (L1PcInstance) cha;
						pc.setAdvenHp(pc.getBaseMaxHp() / 5);
						pc.setAdvenMp(pc.getBaseMaxMp() / 5);
						pc.addMaxHp(pc.getAdvenHp());
						pc.addMaxMp(pc.getAdvenMp());
						pc.sendPackets(new S_HPUpdate(pc.getCurrentHp(), pc.getMaxHp()));
						if (pc.isInParty()) { // パーティー中
							pc.getParty().updateMiniHP(pc);
						}
						pc.sendPackets(new S_MPUpdate(pc.getCurrentMp(), pc.getMaxMp()));
					} else if (_skillId == SKILL_GREATER_HASTE) { // グレーター ヘイスト
						L1PcInstance pc = (L1PcInstance) cha;
						if (pc.getHasteItemEquipped() > 0) {
							continue;
						}
						if (pc.getMoveSpeed() != 2) { // スロー中以外
							pc.setDrink(false);
							pc.setMoveSpeed(1);
							pc.sendPackets(new S_SkillHaste(pc.getId(), 1, _getBuffIconDuration));
							pc.broadcastPacket(new S_SkillHaste(pc.getId(), 1, 0));
						} else { // スロー中
							int skillNum = 0;
							if (pc.hasSkillEffect(SKILL_SLOW)) {
								skillNum = SKILL_SLOW;
							} else if (pc.hasSkillEffect(SKILL_MASS_SLOW)) {
								skillNum = SKILL_MASS_SLOW;
							} else if (pc.hasSkillEffect(SKILL_ENTANGLE)) {
								skillNum = SKILL_ENTANGLE;
							}
							if (skillNum != 0) {
								pc.removeSkillEffect(skillNum);
								pc.removeSkillEffect(SKILL_GREATER_HASTE);
								pc.setMoveSpeed(0);
								continue;
							}
						}
					} else if (_skillId == SKILL_HOLY_WALK
							|| _skillId == SKILL_MOVING_ACCELERATION
							|| _skillId == SKILL_WIND_WALK) {
						L1PcInstance pc = (L1PcInstance) cha;
						if (_skillId == SKILL_HOLY_WALK) {
							pc.sendPackets(new S_ServerMessage(SystemMessageId.$183));
						}
						pc.setBraveSpeed(4);
						pc.sendPackets(new S_SkillBrave(pc.getId(), 4, _getBuffIconDuration));
						pc.broadcastPacket(new S_SkillBrave(pc.getId(), 4, 0));
					} else if (_skillId == SKILL_BLOODLUST) { // ブラッドラスト
						L1PcInstance pc = (L1PcInstance) cha;
						pc.setBraveSpeed(6);
						pc.sendPackets(new S_SkillBrave(pc.getId(), 6,
								_getBuffIconDuration));
						pc.broadcastPacket(new S_SkillBrave(pc.getId(), 6,
								0));
					} else if (_skillId == SKILL_AWAKEN_ANTHARAS) { // 覺醒：アンタラス
						L1PcInstance pc = (L1PcInstance) cha;
						L1Awake.start(pc, _skillId);
					} else if (_skillId == SKILL_AWAKEN_FAFURION) { // 覺醒：パプリオン
						L1PcInstance pc = (L1PcInstance) cha;
						L1Awake.start(pc, _skillId);
					} else if (_skillId == SKILL_AWAKEN_VALAKAS) { // 覺醒：ヴァラカス
						L1PcInstance pc = (L1PcInstance) cha;
						L1Awake.start(pc, _skillId);
					} else if (_skillId == SKILL_ILLUSION_OGRE) { // イリュージョン：オーガ
						L1PcInstance pc = (L1PcInstance) cha;
						pc.addDmgup(4);
						pc.addHitup(4);
						pc.addBowDmgup(4);
						pc.addBowHitup(4);
					} else if (_skillId == SKILL_ILLUSION_LICH) { // イリュージョン：リッチ
						L1PcInstance pc = (L1PcInstance) cha;
						pc.addSp(2);
						pc.sendPackets(new S_SPMR(pc));
					} else if (_skillId == SKILL_ILLUSION_DIA_GOLEM) { // イリュージョン：ダイアモンドゴーレム
						L1PcInstance pc = (L1PcInstance) cha;
						pc.addAc(-20);
					} else if (_skillId == SKILL_ILLUSION_AVATAR) { // イリュージョン：アバター
						L1PcInstance pc = (L1PcInstance) cha;
						pc.addDmgup(10);
						pc.addBowDmgup(10);
					} else if (_skillId == SKILL_INSIGHT) { // インサイト
						L1PcInstance pc = (L1PcInstance) cha;
						pc.addStr(1);
						pc.addCon(1);
						pc.addDex(1);
						pc.addWis(1);
						pc.addInt(1);
					}
				}

				// ●●●● NPCにしか效果のないスキル ●●●●
				if (_targetType == PC_NPC || _targetType == NPC_NPC) {
					// ★★★ ペット系スキル ★★★
					if (_skillId == SKILL_TAME_MONSTER
							&& ((L1MonsterInstance) cha).getNpcTemplate()
									.isTamable()) { // テイミングモンスター
						int petcost = 0;
						Object[] petlist = _user.getPetList().values().toArray();
						for (Object pet : petlist) {
							// 現在のペットコスト
							petcost += ((L1NpcInstance) pet).getPetcost();
						}
						int charisma = _user.getCha();
						if (_player.isElf()) { // エルフ
							if (charisma > 30) { // max count = 7
								charisma = 30;
							}
							charisma += 12;
						} else if (_player.isWizard()) { // ウィザード
							if (charisma > 36) { // max count = 7
								charisma = 36;
							}
							charisma += 6;
						}
						charisma -= petcost;
						if (charisma >= 6) { // ペットコストの確認
							L1SummonInstance summon = new L1SummonInstance(
									_targetNpc, _user, false);
							_target = summon; // ターゲット入替え
						} else {
							_player.sendPackets(new S_ServerMessage(SystemMessageId.$319));
						}
					} else if (_skillId == SKILL_CREATE_ZOMBIE) { // クリエイトゾンビ
						int petcost = 0;
						Object[] petlist = _user.getPetList().values().toArray();
						for (Object pet : petlist) {
							// 現在のペットコスト
							petcost += ((L1NpcInstance) pet).getPetcost();
						}
						int charisma = _user.getCha();
						if (_player.isElf()) { // エルフ
							if (charisma > 30) { // max count = 7
								charisma = 30;
							}
							charisma += 12;
						} else if (_player.isWizard()) { // ウィザード
							if (charisma > 36) { // max count = 7
								charisma = 36;
							}
							charisma += 6;
						}
						charisma -= petcost;
						if (charisma >= 6) { // ペットコストの確認
							L1SummonInstance summon = new L1SummonInstance(
									_targetNpc, _user, true);
							_target = summon; // ターゲット入替え
						} else {
							_player.sendPackets(new S_ServerMessage(SystemMessageId.$319));
						}
					} else if (_skillId == SKILL_REVEAL_WEAKNESS) { // ウィーク エレメンタル
						if (cha instanceof L1MonsterInstance) {
							L1Npc npcTemp = ((L1MonsterInstance) cha)
									.getNpcTemplate();
							int weakAttr = npcTemp.get_weakAttr();
							if ((weakAttr & 1) == 1) { // 地
								cha.broadcastPacket(new S_SkillSound(cha.getId(), 2169));
							}
							if ((weakAttr & 2) == 2) { // 火
								cha.broadcastPacket(new S_SkillSound(cha.getId(), 2166));
							}
							if ((weakAttr & 4) == 4) { // 水
								cha.broadcastPacket(new S_SkillSound(cha.getId(), 2167));
							}
							if ((weakAttr & 8) == 8) { // 風
								cha.broadcastPacket(new S_SkillSound(cha.getId(), 2168));
							}
						}
					} else if (_skillId == SKILL_RETURN_TO_NATURE) { // 釋放元素
						if (Config.RETURN_TO_NATURE
								&& cha instanceof L1SummonInstance&& _player.getZoneType() != 1 && _target.getZoneType() != 1) {//waja change
							L1SummonInstance summon = (L1SummonInstance) cha;
							summon.broadcastPacket(new S_SkillSound(summon.getId(), 2245));
							summon.returnToNature();
						} else {
							if (_user instanceof L1PcInstance) {
								_player.sendPackets(new S_ServerMessage(SystemMessageId.$79));
							}
						}
					}
				}

				// ■■■■ 個別處理ここまで ■■■■

				if (_skill.getType() == L1Skills.TYPE_HEAL
						&& _targetType == PC_NPC && undeadType == 1) {
					dmg *= -1; // もし、アンデットで回復系スキルならばダメージになる。
				}

				if (_skill.getType() == L1Skills.TYPE_HEAL
						&& _targetType == PC_NPC && undeadType == 3) {
					dmg = 0; // もし、アンデット系ボスで回復系スキルならば無效
				}

				if ((cha instanceof L1TowerInstance
						|| cha instanceof L1DoorInstance) && dmg < 0) { // ガーディアンタワー、ドアにヒールを使用
					dmg = 0;
				}

				if (dmg != 0 || drainMana != 0) {
					_magic.commit(dmg, drainMana); // ダメージ系、回復系の值をターゲットにコミットする。
				}

				// ヒール系の他に、別途回復した場合（V-Tなど）
				if (heal > 0) {
					if ((heal + _user.getCurrentHp()) > _user.getMaxHp()) {
						_user.setCurrentHp(_user.getMaxHp());
					} else {
						_user.setCurrentHp(heal + _user.getCurrentHp());
					}
				}

				if (cha instanceof L1PcInstance) { // ターゲットがPCならば、ACとステータスを送信
					L1PcInstance pc = (L1PcInstance) cha;
					pc.turnOnOffLight();
					pc.sendPackets(new S_OwnCharAttrDef(pc));
					pc.sendPackets(new S_OwnCharStatus(pc));
					sendHappenMessage(pc); // ターゲットにメッセージを送信
				}

				addMagicList(cha, false); // ターゲットに魔法の效果時間を設定

				if (cha instanceof L1PcInstance) { // ターゲットがPCならば、ライト狀態を更新
					L1PcInstance pc = (L1PcInstance) cha;
					pc.turnOnOffLight();
				}
			}
			// 修正無所頓形術對血光斗篷的影響 Start
			if (_skillId == SKILL_DETECTION) {
				if (!_player.getInventory().checkEquipped(20062))
					detection(_player);
			}
			// 修正無所頓形術對血光斗篷的影響 End

			if (_skillId == SKILL_COUNTER_DETECTION) { // ディテクション、カウンターディテクション
				detection(_player);
			}

		} catch (Exception e) {
			_log.log(Level.SEVERE, e.getLocalizedMessage(), e);
		}
	}

	/**
	 * キャンセレーションで解除できないスキルかを返す。
	 */
	private boolean isNotCancelable(int skillNum) {
		return skillNum == SKILL_ENCHANT_WEAPON || skillNum == SKILL_BLESSED_ARMOR
				|| skillNum == SKILL_ABSOLUTE_BARRIER || skillNum == SKILL_ADVANCE_SPIRIT
				|| skillNum == SKILL_STUN_SHOCK || skillNum == SKILL_SHADOW_FANG
				|| skillNum == SKILL_REDUCTION_ARMOR || skillNum == SKILL_SOLID_CARRIAGE
				|| skillNum == SKILL_COUNTER_BARRIER || skillNum == SKILL_AWAKEN_ANTHARAS
				|| skillNum == SKILL_AWAKEN_FAFURION || skillNum == SKILL_AWAKEN_VALAKAS;
	}

	private void detection(L1PcInstance pc) {
		if (!pc.isGmInvis() && pc.isInvisble()) { // 自分
			pc.delInvis();
			pc.beginInvisTimer();
		}

		for (L1PcInstance tgt : L1World.getInstance().getVisiblePlayer(pc)) {
			if (!tgt.isGmInvis() && tgt.isInvisble()) {
				tgt.delInvis();
			}
		}
		L1WorldTraps.getInstance().onDetection(pc);
	}

	// ターゲットについて計算する必要があるか返す
	private boolean isTargetCalc(L1Character cha) {
		// 攻擊魔法のNon－PvP判定
		if (_skill.getTarget().equals("attack") && _skillId != 18) { // 攻擊魔法
			if (isPcSummonPet(cha)) { // 對象がPC、サモン、ペット
				if (_player.getZoneType() == 1 || cha.getZoneType() == 1 // 攻擊する側または攻擊される側がセーフティーゾーン
						|| _player.checkNonPvP(_player, cha)) { // Non-PvP設定
					return false;
				}
			}
		}

		// フォグオブスリーピングは自分自身は對象外
		if (_skillId == SKILL_FOG_OF_SLEEPING && _user.getId() == cha.getId()) {
			return false;
		}

		// マススローは自分自身と自分のペットは對象外
		if (_skillId == SKILL_MASS_SLOW) {
			if (_user.getId() == cha.getId()) {
				return false;
			}
			if (cha instanceof L1SummonInstance) {
				L1SummonInstance summon = (L1SummonInstance) cha;
				if (_user.getId() == summon.getMaster().getId()) {
					return false;
				}
			} else if (cha instanceof L1PetInstance) {
				L1PetInstance pet = (L1PetInstance) cha;
				if (_user.getId() == pet.getMaster().getId()) {
					return false;
				}
			}
		}

		// マステレポートは自分自身のみ對象（同時にクラン員もテレポートさせる）
		if (_skillId == SKILL_MASS_TELEPORT) {
			if (_user.getId() != cha.getId()) {
				return false;
			}
		}

		return true;
	}

	// 對象がPC、サモン、ペットかを返す
	private boolean isPcSummonPet(L1Character cha) {
		if (_targetType == PC_PC) { // 對象がPC
			return true;
		}

		if (_targetType == PC_NPC) {
			if (cha instanceof L1SummonInstance) { // 對象がサモン
				L1SummonInstance summon = (L1SummonInstance) cha;
				if (summon.isExsistMaster()) { // マスターが居る
					return true;
				}
			}
			if (cha instanceof L1PetInstance) { // 對象がペット
				return true;
			}
		}
		return false;
	}

	// ターゲットに對して必ず失敗になるか返す
	private boolean isTargetFailure(L1Character cha) {
		boolean isTU = false;
		boolean isErase = false;
		boolean isManaDrain = false;
		int undeadType = 0;

		if (cha instanceof L1TowerInstance || cha instanceof L1DoorInstance) { // ガーディアンタワー、ドアには確率系スキル無效
			return true;
		}

		if (cha instanceof L1PcInstance) { // 對PCの場合
			if (_targetType == PC_PC && _player.checkNonPvP(_player, cha)) { // Non-PvP設定
				L1PcInstance pc = (L1PcInstance) cha;
				if (_player.getId() == pc.getId()
						|| (pc.getClanid() != 0 && _player.getClanid() == pc
								.getClanid())) {
					return false;
				}
				return true;
			}
			return false;
		}

		if (cha instanceof L1MonsterInstance) { // ターンアンデット可能か判定
			isTU = ((L1MonsterInstance) cha).getNpcTemplate().get_IsTU();
		}

		if (cha instanceof L1MonsterInstance) { // イレースマジック可能か判定
			isErase = ((L1MonsterInstance) cha).getNpcTemplate().get_IsErase();
		}

		if (cha instanceof L1MonsterInstance) { // アンデットの判定
			undeadType = ((L1MonsterInstance) cha).getNpcTemplate().get_undead();
		}

		// マナドレインが可能か？
		if (cha instanceof L1MonsterInstance) {
			isManaDrain = true;
		}
		/*
		 * 成功除外條件１：T-Uが成功したが、對象がアンデットではない。 成功除外條件２：T-Uが成功したが、對象にはターンアンデット無效。
		 * 成功除外條件３：スロー、マススロー、マナドレイン、エンタングル、イレースマジック、ウィンドシャックル無效
		 * 成功除外條件４：マナドレインが成功したが、モンスター以外の場合
		 */
		if ((_skillId == SKILL_TURN_UNDEAD && (undeadType == 0 || undeadType == 2))
				|| (_skillId == SKILL_TURN_UNDEAD && isTU == false)
				|| ((_skillId == SKILL_ERASE_MAGIC || _skillId == SKILL_SLOW
						|| _skillId == SKILL_MANA_DRAIN || _skillId == SKILL_MASS_SLOW
						|| _skillId == SKILL_ENTANGLE || _skillId == SKILL_WIND_SHACKLE)
								&& isErase == false)
				|| (_skillId == SKILL_MANA_DRAIN && isManaDrain == false)) {
			return true;
		}
		return false;
	}

	// カウンターマジックが發動したか返す
	private boolean isUseCounterMagic(L1Character cha) {
		// カウンターマジック有效なスキルでカウンターマジック中
		if (_isCounterMagic && cha.hasSkillEffect(SKILL_COUNTER_MAGIC)) {
			cha.removeSkillEffect(SKILL_COUNTER_MAGIC);
			int castgfx = SkillsTable.getInstance().getTemplate(SKILL_COUNTER_MAGIC).getCastGfx();
			cha.broadcastPacket(new S_SkillSound(cha.getId(), castgfx));
			if (cha instanceof L1PcInstance) {
				L1PcInstance pc = (L1PcInstance) cha;
				pc.sendPackets(new S_SkillSound(pc.getId(), castgfx));
			}
			return true;
		}
		return false;
	}

}