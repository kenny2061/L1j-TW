/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307, USA.
 *
 * http://www.gnu.org/copyleft/gpl.html
 */

package net.l1j.server.datatables;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.l1j.Config;
import net.l1j.L1DatabaseFactory;
import net.l1j.server.model.instance.L1PcInstance;
import net.l1j.server.utils.SQLUtil;

public class ChatLogTable {
	private static Logger _log = Logger.getLogger(ChatLogTable.class.getName());

	/*
	 * コード的にはHashMapを利用すべきだが、パフォーマンス上の問題があるかもしれない為、配列で妥協。
	 * HashMapへの變更を檢討する場合は、パフォーマンス上問題が無いか十分注意すること。
	 */
	private final boolean[] loggingConfig = new boolean[15];

	private ChatLogTable() {
		loadConfig();
	}

	private void loadConfig() {
		loggingConfig[0] = Config.LOGGING_CHAT_NORMAL;
		loggingConfig[1] = Config.LOGGING_CHAT_WHISPER;
		loggingConfig[2] = Config.LOGGING_CHAT_SHOUT;
		loggingConfig[3] = Config.LOGGING_CHAT_WORLD;
		loggingConfig[4] = Config.LOGGING_CHAT_CLAN;
		loggingConfig[11] = Config.LOGGING_CHAT_PARTY;
		loggingConfig[13] = Config.LOGGING_CHAT_COMBINED;
		loggingConfig[14] = Config.LOGGING_CHAT_CHAT_PARTY;
	}

	private static ChatLogTable _instance;

	public static ChatLogTable getInstance() {
		if (_instance == null) {
			_instance = new ChatLogTable();
		}
		return _instance;
	}

	private boolean isLoggingTarget(int type) {
		return loggingConfig[type];
	}

	public void storeChat(L1PcInstance pc, L1PcInstance target, String text, int type) {
		if (!isLoggingTarget(type)) {
			return;
		}

		// type
		// 0:通常チャット
		// 1:Whisper
		// 2:叫び
		// 3:全体チャット
		// 4:血盟チャット
		// 11:パーティチャット
		// 13:連合チャット
		// 14:チャットパーティ
		Connection con = null;
		PreparedStatement pstm = null;
		try {
			con = L1DatabaseFactory.getInstance().getConnection();
			if (target != null) {
				pstm = con.prepareStatement("INSERT INTO log_chat (account_name, char_id, name, clan_id, clan_name, locx, locy, mapid, type, target_account_name, target_id, target_name, target_clan_id, target_clan_name, target_locx, target_locy, target_mapid, content, datetime) VALUE (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, SYSDATE())");
				pstm.setString(1, pc.getAccountName());
				pstm.setInt(2, pc.getId());
				pstm.setString(3, pc.isGm() ? "******" : pc.getName());
				pstm.setInt(4, pc.getClanid());
				pstm.setString(5, pc.getClanname());
				pstm.setInt(6, pc.getX());
				pstm.setInt(7, pc.getY());
				pstm.setInt(8, pc.getMapId());
				pstm.setInt(9, type);
				pstm.setString(10, target.getAccountName());
				pstm.setInt(11, target.getId());
				pstm.setString(12, target.getName());
				pstm.setInt(13, target.getClanid());
				pstm.setString(14, target.getClanname());
				pstm.setInt(15, target.getX());
				pstm.setInt(16, target.getY());
				pstm.setInt(17, target.getMapId());
				pstm.setString(18, text);
			} else {
				pstm = con.prepareStatement("INSERT INTO log_chat (account_name, char_id, name, clan_id, clan_name, locx, locy, mapid, type, content, datetime) VALUE (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, SYSDATE())");
				pstm.setString(1, pc.getAccountName());
				pstm.setInt(2, pc.getId());
				pstm.setString(3, pc.isGm() ? "******" : pc.getName());
				pstm.setInt(4, pc.getClanid());
				pstm.setString(5, pc.getClanname());
				pstm.setInt(6, pc.getX());
				pstm.setInt(7, pc.getY());
				pstm.setInt(8, pc.getMapId());
				pstm.setInt(9, type);
				pstm.setString(10, text);
			}
			pstm.execute();
		} catch (SQLException e) {
			_log.log(Level.SEVERE, e.getLocalizedMessage(), e);
		} finally {
			SQLUtil.close(pstm, con);
		}
	}
}
