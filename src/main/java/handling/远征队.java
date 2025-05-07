/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package handling;

import client.MapleCharacter;
import client.MapleClient;
import tools.data.LittleEndianAccessor;

/**
 *
 * @author Administrator
 */
class 远征队 {
    public static void 开设远征队(LittleEndianAccessor slea, MapleClient c) {
         c.getPlayer().dropMessage(5, "远征队功能暂时无法使用。请去地图使用NPC开设远征队！");
    }
  
}
