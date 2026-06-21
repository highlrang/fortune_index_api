package com.hwcompany.fortune_index.common

import com.hwcompany.fortune_index.domain.model.EarthlyBranch
import com.hwcompany.fortune_index.domain.model.HeavenlyStem
import com.hwcompany.fortune_index.saju.FiveElement
import com.hwcompany.fortune_index.saju.YinYang

interface CommonCodeEnum {
    val code: String
    val key: String
}

enum class Sinsung(
    override val code: String,
    override val key: String,
    val koreanName: String,
    val englishName: String
) : CommonCodeEnum {
    BIGYEON("BIGYEON", "sinsung.bigyeon", "비견", "Companion"),
    GEOPJAE("GEOPJAE", "sinsung.geopjae", "겁재", "Rob Wealth"),
    SIKSIN("SIKSIN", "sinsung.siksin", "식신", "Eating God"),
    SANGGWAN("SANGGWAN", "sinsung.sanggwan", "상관", "Hurting Officer"),
    PYEONJAE("PYEONJAE", "sinsung.pyeonjae", "편재", "Indirect Wealth"),
    JEONGJAE("JEONGJAE", "sinsung.jeongjae", "정재", "Direct Wealth"),
    PYEONGWAN("PYEONGWAN", "sinsung.pyeongwan", "편관", "Seven Killings"),
    JEONGGWAN("JEONGGWAN", "sinsung.jeonggwan", "정관", "Direct Officer"),
    PYEONIN("PYEONIN", "sinsung.pyeonin", "편인", "Indirect Resource"),
    JEONGIN("JEONGIN", "sinsung.jeongin", "정인", "Direct Resource")
}

enum class ChineseZodiac(
    override val code: String,
    override val key: String,
    val koreanName: String,
    val chineseCharacter: String,
    val animalName: String,
    val element: FiveElement,
    val yinYang: YinYang,
    val branch: EarthlyBranch
) : CommonCodeEnum {
    JA("JA", "zodiac.ja", "자", "子", "Rat", FiveElement.WATER, YinYang.YANG, EarthlyBranch.JA),
    CHUK("CHUK", "zodiac.chuk", "축", "丑", "Ox", FiveElement.EARTH, YinYang.YIN, EarthlyBranch.CHUK),
    IN("IN", "zodiac.in", "인", "寅", "Tiger", FiveElement.WOOD, YinYang.YANG, EarthlyBranch.IN),
    MYO("MYO", "zodiac.myo", "묘", "卯", "Rabbit", FiveElement.WOOD, YinYang.YIN, EarthlyBranch.MYO),
    JIN("JIN", "zodiac.jin", "진", "辰", "Dragon", FiveElement.EARTH, YinYang.YANG, EarthlyBranch.JIN),
    SA("SA", "zodiac.sa", "사", "巳", "Snake", FiveElement.FIRE, YinYang.YIN, EarthlyBranch.SA),
    O("O", "zodiac.o", "오", "午", "Horse", FiveElement.FIRE, YinYang.YANG, EarthlyBranch.O),
    MI("MI", "zodiac.mi", "미", "未", "Goat", FiveElement.EARTH, YinYang.YIN, EarthlyBranch.MI),
    SIN("SIN", "zodiac.sin", "신", "申", "Monkey", FiveElement.METAL, YinYang.YANG, EarthlyBranch.SIN),
    YU("YU", "zodiac.yu", "유", "酉", "Rooster", FiveElement.METAL, YinYang.YIN, EarthlyBranch.YU),
    SUL("SUL", "zodiac.sul", "술", "戌", "Dog", FiveElement.EARTH, YinYang.YANG, EarthlyBranch.SUL),
    HAE("HAE", "zodiac.hae", "해", "亥", "Pig", FiveElement.WATER, YinYang.YIN, EarthlyBranch.HAE)
}

enum class SajuGanji(
    override val code: String,
    override val key: String,
    val koreanName: String,
    val chineseCharacter: String,
    val stem: HeavenlyStem,
    val chineseZodiac: ChineseZodiac
) : CommonCodeEnum {
    GAPJA("GAPJA", "ganji.gapja", "갑자", "甲子", HeavenlyStem.GAP, ChineseZodiac.JA),
    EULCHUK("EULCHUK", "ganji.eulchuk", "을축", "乙丑", HeavenlyStem.EUL, ChineseZodiac.CHUK),
    BYEONGIN("BYEONGIN", "ganji.byeongin", "병인", "丙寅", HeavenlyStem.BYEONG, ChineseZodiac.IN),
    JEONGMYO("JEONGMYO", "ganji.jeongmyo", "정묘", "丁卯", HeavenlyStem.JEONG, ChineseZodiac.MYO),
    MUJIN("MUJIN", "ganji.mujin", "무진", "戊辰", HeavenlyStem.MU, ChineseZodiac.JIN),
    GISA("GISA", "ganji.gisa", "기사", "己巳", HeavenlyStem.GI, ChineseZodiac.SA),
    GYEONGO("GYEONGO", "ganji.gyeongo", "경오", "庚午", HeavenlyStem.GYEONG, ChineseZodiac.O),
    SINMI("SINMI", "ganji.sinmi", "신미", "辛未", HeavenlyStem.SIN, ChineseZodiac.MI),
    IMSIN("IMSIN", "ganji.imsin", "임신", "壬申", HeavenlyStem.IM, ChineseZodiac.SIN),
    GYEYU("GYEYU", "ganji.gyeyu", "계유", "癸酉", HeavenlyStem.GYE, ChineseZodiac.YU),
    GAPSUL("GAPSUL", "ganji.gapsul", "갑술", "甲戌", HeavenlyStem.GAP, ChineseZodiac.SUL),
    EULHAE("EULHAE", "ganji.eulhae", "을해", "乙亥", HeavenlyStem.EUL, ChineseZodiac.HAE),
    BYEONGJA("BYEONGJA", "ganji.byeongja", "병자", "丙子", HeavenlyStem.BYEONG, ChineseZodiac.JA),
    JEONGCHUK("JEONGCHUK", "ganji.jeongchuk", "정축", "丁丑", HeavenlyStem.JEONG, ChineseZodiac.CHUK),
    MUIN("MUIN", "ganji.muin", "무인", "戊寅", HeavenlyStem.MU, ChineseZodiac.IN),
    GIMYO("GIMYO", "ganji.gimyo", "기묘", "己卯", HeavenlyStem.GI, ChineseZodiac.MYO),
    GYEONGJIN("GYEONGJIN", "ganji.gyeongjin", "경진", "庚辰", HeavenlyStem.GYEONG, ChineseZodiac.JIN),
    SINSA("SINSA", "ganji.sinsa", "신사", "辛巳", HeavenlyStem.SIN, ChineseZodiac.SA),
    IMO("IMO", "ganji.imo", "임오", "壬午", HeavenlyStem.IM, ChineseZodiac.O),
    GYEMI("GYEMI", "ganji.gyemi", "계미", "癸未", HeavenlyStem.GYE, ChineseZodiac.MI),
    GAPSIN("GAPSIN", "ganji.gapsin", "갑신", "甲申", HeavenlyStem.GAP, ChineseZodiac.SIN),
    EULYU("EULYU", "ganji.eulyu", "을유", "乙酉", HeavenlyStem.EUL, ChineseZodiac.YU),
    BYEONGSUL("BYEONGSUL", "ganji.byeongsul", "병술", "丙戌", HeavenlyStem.BYEONG, ChineseZodiac.SUL),
    JEONGHAE("JEONGHAE", "ganji.jeonghae", "정해", "丁亥", HeavenlyStem.JEONG, ChineseZodiac.HAE),
    MUJA("MUJA", "ganji.muja", "무자", "戊子", HeavenlyStem.MU, ChineseZodiac.JA),
    GICHUK("GICHUK", "ganji.gichuk", "기축", "己丑", HeavenlyStem.GI, ChineseZodiac.CHUK),
    GYEONGIN("GYEONGIN", "ganji.gyeongin", "경인", "庚寅", HeavenlyStem.GYEONG, ChineseZodiac.IN),
    SINMYO("SINMYO", "ganji.sinmyo", "신묘", "辛卯", HeavenlyStem.SIN, ChineseZodiac.MYO),
    IMJIN("IMJIN", "ganji.imjin", "임진", "壬辰", HeavenlyStem.IM, ChineseZodiac.JIN),
    GYESA("GYESA", "ganji.gyesa", "계사", "癸巳", HeavenlyStem.GYE, ChineseZodiac.SA),
    GAPO("GAPO", "ganji.gapo", "갑오", "甲午", HeavenlyStem.GAP, ChineseZodiac.O),
    EULMI("EULMI", "ganji.eulmi", "을미", "乙未", HeavenlyStem.EUL, ChineseZodiac.MI),
    BYEONGSIN("BYEONGSIN", "ganji.byeongsin", "병신", "丙申", HeavenlyStem.BYEONG, ChineseZodiac.SIN),
    JEONGYU("JEONGYU", "ganji.jeongyu", "정유", "丁酉", HeavenlyStem.JEONG, ChineseZodiac.YU),
    MUSUL("MUSUL", "ganji.musul", "무술", "戊戌", HeavenlyStem.MU, ChineseZodiac.SUL),
    GIHAE("GIHAE", "ganji.gihae", "기해", "己亥", HeavenlyStem.GI, ChineseZodiac.HAE),
    GYEONGJA("GYEONGJA", "ganji.gyeongja", "경자", "庚子", HeavenlyStem.GYEONG, ChineseZodiac.JA),
    SINCHUK("SINCHUK", "ganji.sinchuk", "신축", "辛丑", HeavenlyStem.SIN, ChineseZodiac.CHUK),
    IMIN("IMIN", "ganji.imin", "임인", "壬寅", HeavenlyStem.IM, ChineseZodiac.IN),
    GYEMYO("GYEMYO", "ganji.gyemyo", "계묘", "癸卯", HeavenlyStem.GYE, ChineseZodiac.MYO),
    GAPJIN("GAPJIN", "ganji.gapjin", "갑진", "甲辰", HeavenlyStem.GAP, ChineseZodiac.JIN),
    EULSA("EULSA", "ganji.eulsa", "을사", "乙巳", HeavenlyStem.EUL, ChineseZodiac.SA),
    BYEONGO("BYEONGO", "ganji.byeongo", "병오", "丙午", HeavenlyStem.BYEONG, ChineseZodiac.O),
    JEONGMI("JEONGMI", "ganji.jeongmi", "정미", "丁未", HeavenlyStem.JEONG, ChineseZodiac.MI),
    MUSIN("MUSIN", "ganji.musin", "무신", "戊申", HeavenlyStem.MU, ChineseZodiac.SIN),
    GIYU("GIYU", "ganji.giyu", "기유", "己酉", HeavenlyStem.GI, ChineseZodiac.YU),
    GYEONGSUL("GYEONGSUL", "ganji.gyeongsul", "경술", "庚戌", HeavenlyStem.GYEONG, ChineseZodiac.SUL),
    SINHAE("SINHAE", "ganji.sinhae", "신해", "辛亥", HeavenlyStem.SIN, ChineseZodiac.HAE),
    IMJA("IMJA", "ganji.imja", "임자", "壬子", HeavenlyStem.IM, ChineseZodiac.JA),
    GYECHUK("GYECHUK", "ganji.gyechuk", "계축", "癸丑", HeavenlyStem.GYE, ChineseZodiac.CHUK),
    GAPIN("GAPIN", "ganji.gapin", "갑인", "甲寅", HeavenlyStem.GAP, ChineseZodiac.IN),
    EULMYO("EULMYO", "ganji.eulmyo", "을묘", "乙卯", HeavenlyStem.EUL, ChineseZodiac.MYO),
    BYEONGJIN("BYEONGJIN", "ganji.byeongjin", "병진", "丙辰", HeavenlyStem.BYEONG, ChineseZodiac.JIN),
    JEONGSA("JEONGSA", "ganji.jeongsa", "정사", "丁巳", HeavenlyStem.JEONG, ChineseZodiac.SA),
    MUO("MUO", "ganji.muo", "무오", "戊午", HeavenlyStem.MU, ChineseZodiac.O),
    GIMI("GIMI", "ganji.gimi", "기미", "己未", HeavenlyStem.GI, ChineseZodiac.MI),
    GYEONGSIN("GYEONGSIN", "ganji.gyeongsin", "경신", "庚申", HeavenlyStem.GYEONG, ChineseZodiac.SIN),
    SINYU("SINYU", "ganji.sinyu", "신유", "辛酉", HeavenlyStem.SIN, ChineseZodiac.YU),
    IMSUL("IMSUL", "ganji.imsul", "임술", "壬戌", HeavenlyStem.IM, ChineseZodiac.SUL),
    GYEHAE("GYEHAE", "ganji.gyehae", "계해", "癸亥", HeavenlyStem.GYE, ChineseZodiac.HAE);

    companion object {
        fun of(stem: HeavenlyStem, chineseZodiac: ChineseZodiac): SajuGanji =
            entries.firstOrNull { it.stem == stem && it.chineseZodiac == chineseZodiac }
                ?: throw IllegalArgumentException("천간과 지지에 맞는 간지를 찾을 수 없습니다: stem=$stem, chineseZodiac=$chineseZodiac")
    }
}
