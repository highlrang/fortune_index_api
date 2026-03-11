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

enum class Zodiac(
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
    val zodiac: Zodiac
) : CommonCodeEnum {
    GAPJA("GAPJA", "ganji.gapja", "갑자", "甲子", HeavenlyStem.GAP, Zodiac.JA),
    EULCHUK("EULCHUK", "ganji.eulchuk", "을축", "乙丑", HeavenlyStem.EUL, Zodiac.CHUK),
    BYEONGIN("BYEONGIN", "ganji.byeongin", "병인", "丙寅", HeavenlyStem.BYEONG, Zodiac.IN),
    JEONGMYO("JEONGMYO", "ganji.jeongmyo", "정묘", "丁卯", HeavenlyStem.JEONG, Zodiac.MYO),
    MUJIN("MUJIN", "ganji.mujin", "무진", "戊辰", HeavenlyStem.MU, Zodiac.JIN),
    GISA("GISA", "ganji.gisa", "기사", "己巳", HeavenlyStem.GI, Zodiac.SA),
    GYEONGO("GYEONGO", "ganji.gyeongo", "경오", "庚午", HeavenlyStem.GYEONG, Zodiac.O),
    SINMI("SINMI", "ganji.sinmi", "신미", "辛未", HeavenlyStem.SIN, Zodiac.MI),
    IMSIN("IMSIN", "ganji.imsin", "임신", "壬申", HeavenlyStem.IM, Zodiac.SIN),
    GYEYU("GYEYU", "ganji.gyeyu", "계유", "癸酉", HeavenlyStem.GYE, Zodiac.YU),
    GAPSUL("GAPSUL", "ganji.gapsul", "갑술", "甲戌", HeavenlyStem.GAP, Zodiac.SUL),
    EULHAE("EULHAE", "ganji.eulhae", "을해", "乙亥", HeavenlyStem.EUL, Zodiac.HAE),
    BYEONGJA("BYEONGJA", "ganji.byeongja", "병자", "丙子", HeavenlyStem.BYEONG, Zodiac.JA),
    JEONGCHUK("JEONGCHUK", "ganji.jeongchuk", "정축", "丁丑", HeavenlyStem.JEONG, Zodiac.CHUK),
    MUIN("MUIN", "ganji.muin", "무인", "戊寅", HeavenlyStem.MU, Zodiac.IN),
    GIMYO("GIMYO", "ganji.gimyo", "기묘", "己卯", HeavenlyStem.GI, Zodiac.MYO),
    GYEONGJIN("GYEONGJIN", "ganji.gyeongjin", "경진", "庚辰", HeavenlyStem.GYEONG, Zodiac.JIN),
    SINSA("SINSA", "ganji.sinsa", "신사", "辛巳", HeavenlyStem.SIN, Zodiac.SA),
    IMO("IMO", "ganji.imo", "임오", "壬午", HeavenlyStem.IM, Zodiac.O),
    GYEMI("GYEMI", "ganji.gyemi", "계미", "癸未", HeavenlyStem.GYE, Zodiac.MI),
    GAPSIN("GAPSIN", "ganji.gapsin", "갑신", "甲申", HeavenlyStem.GAP, Zodiac.SIN),
    EULYU("EULYU", "ganji.eulyu", "을유", "乙酉", HeavenlyStem.EUL, Zodiac.YU),
    BYEONGSUL("BYEONGSUL", "ganji.byeongsul", "병술", "丙戌", HeavenlyStem.BYEONG, Zodiac.SUL),
    JEONGHAE("JEONGHAE", "ganji.jeonghae", "정해", "丁亥", HeavenlyStem.JEONG, Zodiac.HAE),
    MUJA("MUJA", "ganji.muja", "무자", "戊子", HeavenlyStem.MU, Zodiac.JA),
    GICHUK("GICHUK", "ganji.gichuk", "기축", "己丑", HeavenlyStem.GI, Zodiac.CHUK),
    GYEONGIN("GYEONGIN", "ganji.gyeongin", "경인", "庚寅", HeavenlyStem.GYEONG, Zodiac.IN),
    SINMYO("SINMYO", "ganji.sinmyo", "신묘", "辛卯", HeavenlyStem.SIN, Zodiac.MYO),
    IMJIN("IMJIN", "ganji.imjin", "임진", "壬辰", HeavenlyStem.IM, Zodiac.JIN),
    GYESA("GYESA", "ganji.gyesa", "계사", "癸巳", HeavenlyStem.GYE, Zodiac.SA),
    GAPO("GAPO", "ganji.gapo", "갑오", "甲午", HeavenlyStem.GAP, Zodiac.O),
    EULMI("EULMI", "ganji.eulmi", "을미", "乙未", HeavenlyStem.EUL, Zodiac.MI),
    BYEONGSIN("BYEONGSIN", "ganji.byeongsin", "병신", "丙申", HeavenlyStem.BYEONG, Zodiac.SIN),
    JEONGYU("JEONGYU", "ganji.jeongyu", "정유", "丁酉", HeavenlyStem.JEONG, Zodiac.YU),
    MUSUL("MUSUL", "ganji.musul", "무술", "戊戌", HeavenlyStem.MU, Zodiac.SUL),
    GIHAE("GIHAE", "ganji.gihae", "기해", "己亥", HeavenlyStem.GI, Zodiac.HAE),
    GYEONGJA("GYEONGJA", "ganji.gyeongja", "경자", "庚子", HeavenlyStem.GYEONG, Zodiac.JA),
    SINCHUK("SINCHUK", "ganji.sinchuk", "신축", "辛丑", HeavenlyStem.SIN, Zodiac.CHUK),
    IMIN("IMIN", "ganji.imin", "임인", "壬寅", HeavenlyStem.IM, Zodiac.IN),
    GYEMYO("GYEMYO", "ganji.gyemyo", "계묘", "癸卯", HeavenlyStem.GYE, Zodiac.MYO),
    GAPJIN("GAPJIN", "ganji.gapjin", "갑진", "甲辰", HeavenlyStem.GAP, Zodiac.JIN),
    EULSA("EULSA", "ganji.eulsa", "을사", "乙巳", HeavenlyStem.EUL, Zodiac.SA),
    BYEONGO("BYEONGO", "ganji.byeongo", "병오", "丙午", HeavenlyStem.BYEONG, Zodiac.O),
    JEONGMI("JEONGMI", "ganji.jeongmi", "정미", "丁未", HeavenlyStem.JEONG, Zodiac.MI),
    MUSIN("MUSIN", "ganji.musin", "무신", "戊申", HeavenlyStem.MU, Zodiac.SIN),
    GIYU("GIYU", "ganji.giyu", "기유", "己酉", HeavenlyStem.GI, Zodiac.YU),
    GYEONGSUL("GYEONGSUL", "ganji.gyeongsul", "경술", "庚戌", HeavenlyStem.GYEONG, Zodiac.SUL),
    SINHAE("SINHAE", "ganji.sinhae", "신해", "辛亥", HeavenlyStem.SIN, Zodiac.HAE),
    IMJA("IMJA", "ganji.imja", "임자", "壬子", HeavenlyStem.IM, Zodiac.JA),
    GYECHUK("GYECHUK", "ganji.gyechuk", "계축", "癸丑", HeavenlyStem.GYE, Zodiac.CHUK),
    GAPIN("GAPIN", "ganji.gapin", "갑인", "甲寅", HeavenlyStem.GAP, Zodiac.IN),
    EULMYO("EULMYO", "ganji.eulmyo", "을묘", "乙卯", HeavenlyStem.EUL, Zodiac.MYO),
    BYEONGJIN("BYEONGJIN", "ganji.byeongjin", "병진", "丙辰", HeavenlyStem.BYEONG, Zodiac.JIN),
    JEONGSA("JEONGSA", "ganji.jeongsa", "정사", "丁巳", HeavenlyStem.JEONG, Zodiac.SA),
    MUO("MUO", "ganji.muo", "무오", "戊午", HeavenlyStem.MU, Zodiac.O),
    GIMI("GIMI", "ganji.gimi", "기미", "己未", HeavenlyStem.GI, Zodiac.MI),
    GYEONGSIN("GYEONGSIN", "ganji.gyeongsin", "경신", "庚申", HeavenlyStem.GYEONG, Zodiac.SIN),
    SINYU("SINYU", "ganji.sinyu", "신유", "辛酉", HeavenlyStem.SIN, Zodiac.YU),
    IMSUL("IMSUL", "ganji.imsul", "임술", "壬戌", HeavenlyStem.IM, Zodiac.SUL),
    GYEHAE("GYEHAE", "ganji.gyehae", "계해", "癸亥", HeavenlyStem.GYE, Zodiac.HAE);

    companion object {
        fun of(stem: HeavenlyStem, zodiac: Zodiac): SajuGanji =
            entries.firstOrNull { it.stem == stem && it.zodiac == zodiac }
                ?: throw IllegalArgumentException("No ganji found for stem=$stem, zodiac=$zodiac")
    }
}
