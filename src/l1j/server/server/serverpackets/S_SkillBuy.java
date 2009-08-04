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

package l1j.server.server.serverpackets;

import static l1j.server.server.Opcodes.S_OPCODE_SKILLBUY;

import l1j.server.server.clientpackets.C_SkillBuyOK;
import l1j.server.server.model.Instance.L1PcInstance;


// Referenced classes of package l1j.server.server.serverpackets:
// ServerBasePacket

public class S_SkillBuy extends ServerBasePacket
{
	public S_SkillBuy(int objid, L1PcInstance Pc)
	{
		int SkillAmount = 0;
		
		for (int i = 1; i <= 24; i++)
			if (!C_SkillBuyOK.SpellCheck(Pc, i))
				SkillAmount++;
		
		writeC(S_OPCODE_SKILLBUY);
		writeD(0x00000064);
		writeH(SkillAmount);
		
		for (int i = 1; i <= 24; i++)
			if (!C_SkillBuyOK.SpellCheck(Pc, i))
				writeD(i - 1);
		
		if (SkillAmount == 0)
			writeD(objid);
	}

	@Override
	public byte[] getContent()
	{
		return getBytes();
	}
}
