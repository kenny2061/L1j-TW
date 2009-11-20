package l1j.server.server.model;

import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import l1j.server.server.model.Instance.L1PcInstance;
import l1j.server.server.skills.SkillId;
import l1j.server.server.serverpackets.S_SkillSound; //娃娃回血效果

public class HpRegenerationByDoll extends TimerTask {
	private static Logger _log = Logger.getLogger(HpRegenerationByDoll.class
			.getName());

	private final L1PcInstance _pc;

	public HpRegenerationByDoll(L1PcInstance pc) {
		_pc = pc;
	}

	@Override
	public void run() {
		try {
			if (_pc.isDead()) {
				return;
			}
			regenHp();
		} catch (Throwable e) {
			_log.log(Level.WARNING, e.getLocalizedMessage(), e);
		}
	}

	public void regenHp() {
		int newHp = _pc.getCurrentHp();
		if (newHp <= 0) {
			newHp = 0;
		} else if (_pc.get_food() >= 40 && isOverWeight(_pc)) { // 40 == 飽食度 17%
			newHp += 40;
			_pc.sendPackets(new S_SkillSound(_pc.getId(), 6321));
			_pc.broadcastPacket(new S_SkillSound(_pc.getId(), 6321));
			_pc.setCurrentHp(newHp);
		} else {
			System.out.println("HpRegenerationByDoll.java 『娃娃回血效果』異常 。 _pc.get_food() : " + _pc.get_food() + "isOverWeight(_pc) : " + isOverWeight(_pc) + "_pc.getCurrentHp() : " + _pc.getCurrentHp());
		}
	}

	private boolean isOverWeight(L1PcInstance pc) {
		if (pc.hasSkillEffect(SkillId.SKILL_EXOTIC_VITALIZE)
				|| pc.hasSkillEffect(SkillId.SKILL_ADDITIONAL_FIRE)) {
			return false;
		}
		return (14 < pc.getInventory().getWeight240()) ? true : false;
	}
	}