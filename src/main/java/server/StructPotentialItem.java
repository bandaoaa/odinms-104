package server;

public class StructPotentialItem {

    public byte incSTR;
    public byte incDEX;
    public byte incINT;
    public byte incLUK;
    public byte incACC;
    public byte incEVA;
    public byte incSpeed;
    public byte incJump;
    public byte incPAD;
    public byte incMAD;
    public byte incPDD;
    public byte incMDD;
    public byte prop;
    public byte time;
    public byte incSTRr;
    public byte incDEXr;
    public byte incINTr;
    public byte incLUKr;
    public byte incMHPr;
    public byte incMMPr;
    public byte incACCr;
    public byte incEVAr;
    public byte incPADr;
    public byte incMADr;
    public byte incPDDr;
    public byte incMDDr;
    public byte incCr;
    public byte incDAMr;
    public byte RecoveryHP;
    public byte RecoveryMP;
    public byte HP;
    public byte MP;
    public byte level;
    public byte ignoreTargetDEF;
    public byte ignoreDAM;
    public byte DAMreflect;
    public byte mpconReduce;
    public byte mpRestore;
    public byte incMesoProp;
    public byte incRewardProp;
    public byte incAllskill;
    public byte ignoreDAMr;
    public byte RecoveryUP;
    public boolean boss;
    public short incMHP;
    public short incMMP;
    public short attackType;
    public short skillID;
    public int optionType;
    public int reqLevel;
    public int potentialID;
    public String face;

    @Override
    public String toString() {
        StringBuilder ret = new StringBuilder();
        if (this.incMesoProp > 0) {
            ret.append("金币获得量 : ");
            ret.append(this.incMesoProp);
            ret.append(" ");
        }
        if (this.incRewardProp > 0) {
            ret.append("物品获得概率 : ");
            ret.append(this.incRewardProp);
            ret.append(" ");
        }
        if (this.prop > 0) {
            ret.append("自动拾取 : ");
            ret.append(this.prop);
            ret.append(" ");
        }
        if (this.time > 0) {
            ret.append("Duration(not coded): ");
            ret.append(this.time);
            ret.append(" ");
        }
        if (this.attackType > 0) {
            ret.append("Attack Type(not coded): ");
            ret.append(this.attackType);
            ret.append(" ");
        }
        if (this.incAllskill > 0) {
            ret.append("所有技能等级: ");
            ret.append(this.incAllskill);
            ret.append(" ");
        }
        if (this.skillID > 0) {
            ret.append("Gives SKILL: ");
            ret.append(this.skillID);
            ret.append(" ");
        }
        if (this.boss) {
            ret.append("BOSS ONLY, ");
        }
        if (this.face.length() > 0) {
            ret.append("Face Expression: ");
            ret.append(this.face);
            ret.append(" ");
        }
        if (this.RecoveryUP > 0) {
            ret.append("Gives Recovery % on potions: ");
            ret.append(this.RecoveryUP);
            ret.append(" ");
        }
        if (this.DAMreflect > 0) {
            ret.append("反射受到伤害: ");
            ret.append(this.DAMreflect);
            ret.append(" ");
        }
        if (this.mpconReduce > 0) {
            ret.append("较少技能消耗的MP: ");
            ret.append(this.mpconReduce);
            ret.append(" ");
        }
        if (this.ignoreTargetDEF > 0) {
            ret.append("Ignores Monster DEF %: ");
            ret.append(this.ignoreTargetDEF);
            ret.append(" ");
        }
        if (this.RecoveryHP > 0) {
            ret.append("Recovers HP: ");
            ret.append(this.RecoveryHP);
            ret.append(" ");
        }
        if (this.RecoveryMP > 0) {
            ret.append("Recovers MP: ");
            ret.append(this.RecoveryMP);
            ret.append(" ");
        }
        if (this.HP > 0) {
            ret.append("Recovers HP: ");
            ret.append(this.HP);
            ret.append(" ");
        }
        if (this.MP > 0) {
            ret.append("Recovers MP: ");
            ret.append(this.MP);
            ret.append(" ");
        }
        if (this.mpRestore > 0) {
            ret.append("Recovers MP: ");
            ret.append(this.mpRestore);
            ret.append(" ");
        }
        if (this.ignoreDAM > 0) {
            ret.append("Ignores Monster Damage: ");
            ret.append(this.ignoreDAM);
            ret.append(" ");
        }
        if (this.ignoreDAMr > 0) {
            ret.append("Ignores Monster Damage %: ");
            ret.append(this.ignoreDAMr);
            ret.append(" ");
        }
        if (this.incMHP > 0) {
            ret.append("MaxHP : ");
            ret.append(this.incMHP);
            ret.append(" ");
        }
        if (this.incMMP > 0) {
            ret.append("MaxMP : ");
            ret.append(this.incMMP);
            ret.append(" ");
        }
        if (this.incMHPr > 0) {
            ret.append("MaxHP %: ");
            ret.append(this.incMHPr);
            ret.append(" ");
        }
        if (this.incMMPr > 0) {
            ret.append("MaxMP %: ");
            ret.append(this.incMMPr);
            ret.append(" ");
        }
        if (this.incSTR > 0) {
            ret.append("力量 : ");
            ret.append(this.incSTR);
            ret.append(" ");
        }
        if (this.incDEX > 0) {
            ret.append("敏捷 : ");
            ret.append(this.incDEX);
            ret.append(" ");
        }
        if (this.incINT > 0) {
            ret.append("智力 : ");
            ret.append(this.incINT);
            ret.append(" ");
        }
        if (this.incLUK > 0) {
            ret.append("运气 : ");
            ret.append(this.incLUK);
            ret.append(" ");
        }
        if (this.incACC > 0) {
            ret.append("命中值 : ");
            ret.append(this.incACC);
            ret.append(" ");
        }
        if (this.incEVA > 0) {
            ret.append("回避值 : ");
            ret.append(this.incEVA);
            ret.append(" ");
        }
        if (this.incSpeed > 0) {
            ret.append("移动速度 : ");
            ret.append(this.incSpeed);
            ret.append(" ");
        }
        if (this.incJump > 0) {
            ret.append("跳跃力 : ");
            ret.append(this.incJump);
            ret.append(" ");
        }
        if (this.incPAD > 0) {
            ret.append("攻击力 : ");
            ret.append(this.incPAD);
            ret.append(" ");
        }
        if (this.incMAD > 0) {
            ret.append("魔法攻击力 : ");
            ret.append(this.incMAD);
            ret.append(" ");
        }
        if (this.incPDD > 0) {
            ret.append("物理防御力 : ");
            ret.append(this.incPDD);
            ret.append(" ");
        }
        if (this.incMDD > 0) {
            ret.append("魔法防御力 : ");
            ret.append(this.incMDD);
            ret.append(" ");
        }
        if (this.incSTRr > 0) {
            ret.append("力量 %: ");
            ret.append(this.incSTRr);
            ret.append(" ");
        }
        if (this.incDEXr > 0) {
            ret.append("敏捷 %: ");
            ret.append(this.incDEXr);
            ret.append(" ");
        }
        if (this.incINTr > 0) {
            ret.append("智力 %: ");
            ret.append(this.incINTr);
            ret.append(" ");
        }
        if (this.incLUKr > 0) {
            ret.append("运气 %: ");
            ret.append(this.incLUKr);
            ret.append(" ");
        }
        if (this.incACCr > 0) {
            ret.append("命中值 %: ");
            ret.append(this.incACCr);
            ret.append(" ");
        }
        if (this.incEVAr > 0) {
            ret.append("回避值 %: ");
            ret.append(this.incEVAr);
            ret.append(" ");
        }
        if (this.incPADr > 0) {
            ret.append("攻击力 %: ");
            ret.append(this.incPADr);
            ret.append(" ");
        }
        if (this.incMADr > 0) {
            ret.append("魔法攻击力 %: ");
            ret.append(this.incMADr);
            ret.append(" ");
        }
        if (this.incPDDr > 0) {
            ret.append("物理防御力 %: ");
            ret.append(this.incPDDr);
            ret.append(" ");
        }
        if (this.incMDDr > 0) {
            ret.append("魔法防御力 %: ");
            ret.append(this.incMDDr);
            ret.append(" ");
        }
        if (this.incCr > 0) {
            ret.append("爆击率 %: ");
            ret.append(this.incCr);
            ret.append(" ");
        }
        if (this.incDAMr > 0) {
            ret.append("总伤害 %: ");
            ret.append(this.incDAMr);
            ret.append(" ");
        }
        if (this.level > 0) {
            ret.append("Level: ");
            ret.append(this.level);
            ret.append(" ");
        }
        return ret.toString();
    }
}