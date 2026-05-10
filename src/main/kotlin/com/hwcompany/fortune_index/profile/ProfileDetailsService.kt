package com.hwcompany.fortune_index.profile

import com.hwcompany.fortune_index.astrology.AstrologyService
import com.hwcompany.fortune_index.common.SajuGanji
import com.hwcompany.fortune_index.domain.model.EarthlyBranch
import com.hwcompany.fortune_index.domain.model.HeavenlyStem
import com.hwcompany.fortune_index.domain.model.WesternZodiacSign
import com.hwcompany.fortune_index.domain.model.labelKo
import com.hwcompany.fortune_index.history.UserRepository
import com.hwcompany.fortune_index.saju.FiveElementBalance
import com.hwcompany.fortune_index.saju.Pillar
import com.hwcompany.fortune_index.saju.SajuCoreEnergy
import com.hwcompany.fortune_index.saju.SajuAnalyzer
import com.hwcompany.fortune_index.saju.SajuInterpretationCategory
import com.hwcompany.fortune_index.saju.SajuInterpretationService
import com.hwcompany.fortune_index.saju.SajuPersistenceService
import com.hwcompany.fortune_index.saju.SajuResultRepository
import com.hwcompany.fortune_index.saju.TenStar
import com.hwcompany.fortune_index.tarot.DEFAULT_TAROT_DECK_VERSION_ID
import com.hwcompany.fortune_index.tarot.TarotArcanaType
import com.hwcompany.fortune_index.tarot.TarotBirthCardInterpretation
import com.hwcompany.fortune_index.tarot.TarotBirthCardRepository
import com.hwcompany.fortune_index.tarot.TarotCard
import com.hwcompany.fortune_index.tarot.TarotCardMetadataEntity
import com.hwcompany.fortune_index.tarot.TarotCardMetadataRepository
import com.hwcompany.fortune_index.tarot.TarotDeckRole
import com.hwcompany.fortune_index.tarot.TarotDeckVersionRepository
import com.hwcompany.fortune_index.tarot.resolveBirthTarotCard
import com.hwcompany.fortune_index.tarot.toInterpretation
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException

@Service
class ProfileDetailsService(
    private val userRepository: UserRepository,
    private val sajuAnalyzer: SajuAnalyzer,
    private val sajuResultRepository: SajuResultRepository,
    private val sajuPersistenceService: SajuPersistenceService,
    private val astrologyService: AstrologyService,
    private val tarotCardMetadataRepository: TarotCardMetadataRepository,
    private val tarotBirthCardRepository: TarotBirthCardRepository,
    private val tarotDeckVersionRepository: TarotDeckVersionRepository,
    private val sajuInterpretationService: SajuInterpretationService
) {
    @Transactional(readOnly = true)
    fun getProfileDetails(userId: Long): MyProfileDetailsResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "user not found: $userId") }

        val birthTarot = runCatching {
            buildBirthTarot(
                cardCode = user.birthTarotCardCode ?: resolveBirthTarotCard(user.birthInfo.birthDate.toString()).code,
                preferredDeckVersionId = user.preferredTarotDeckId
            )
        }.onFailure { ex ->
            logger.warn(
                "Failed to build birth tarot. userId={}, birthDate={}, preferredDeckVersionId={}",
                user.id,
                user.birthInfo.birthDate,
                user.preferredTarotDeckId,
                ex
            )
        }.getOrNull()

        val saju = runCatching {
            val referenceDateTime = sajuResultRepository.findTopByUserIdOrderByAnalyzedAtDesc(requireNotNull(user.id))
                ?.analyzedAt
                ?: sajuPersistenceService.saveInitialResult(user).analyzedAt
            val birthDateTime = LocalDateTime.of(
                user.birthInfo.birthDate,
                user.birthInfo.birthTime ?: DEFAULT_BIRTH_TIME
            )
            buildSajuProfile(
                consultingResult = sajuAnalyzer.analyzeForConsulting(
                    birthDateTime = birthDateTime,
                    referenceDateTime = referenceDateTime,
                    zoneId = DEFAULT_ZONE_ID,
                    gender = user.gender
                )
            )
        }.getOrNull()

        val astrology = runCatching {
            val birthTime = user.birthInfo.birthTime
            val latitude = user.birthInfo.birthLatitude
            val longitude = user.birthInfo.birthLongitude
            if (birthTime == null || latitude == null || longitude == null) {
                null
            } else {
                astrologyService.calculateNatalChart(
                    birthDate = user.birthInfo.birthDate,
                    birthTime = birthTime,
                    latitude = latitude,
                    longitude = longitude
                ).copy(transits = emptyList())
            }
        }.onFailure { ex ->
            logger.warn(
                "Failed to build astrology profile. userId={}, birthDate={}, birthTime={}, latitude={}, longitude={}",
                user.id,
                user.birthInfo.birthDate,
                user.birthInfo.birthTime,
                user.birthInfo.birthLatitude,
                user.birthInfo.birthLongitude,
                ex
            )
        }.getOrNull()

        return MyProfileDetailsResponse(
            birthTarot = birthTarot,
            saju = saju,
            zodiac = buildZodiacProfile(user.westernZodiac ?: WesternZodiacSign.from(user.birthInfo.birthDate)),
            astrology = astrology
        )
    }

    private fun buildZodiacProfile(sign: WesternZodiacSign): ZodiacProfileResponse {
        val detail = zodiacProfileDetails.getValue(sign)
        return ZodiacProfileResponse(
            sign = sign.sign,
            englishName = sign.englishName,
            dateRange = sign.dateRange,
            element = sign.element,
            elementDescription = zodiacElementDescriptions.getValue(sign.element),
            keyword = sign.keyword,
            keywordDescription = sign.keywordDescription(),
            summary = sign.profileSummary(),
            traits = sign.traits,
            traitDetails = sign.traitDetails(),
            strengths = detail.strengths,
            cautions = detail.cautions,
            moneyStyle = detail.moneyStyle,
            investmentTendency = detail.investmentTendency,
            careTip = detail.careTip
        )
    }

    private data class ZodiacProfileDetail(
        val strengths: List<String>,
        val cautions: List<String>,
        val moneyStyle: String,
        val investmentTendency: String,
        val careTip: String
    )

    private val zodiacElementDescriptions = mapOf(
        "불" to "불 원소는 빠른 결단, 추진력, 확신의 에너지를 뜻해요. 재물 흐름에서는 기회가 보일 때 먼저 움직이는 힘으로 나타나요.",
        "흙" to "흙 원소는 안정감, 현실성, 축적의 에너지를 뜻해요. 재물 흐름에서는 지속 가능성과 실속을 확인하려는 감각으로 나타나요.",
        "바람" to "바람 원소는 정보, 관점, 소통의 에너지를 뜻해요. 재물 흐름에서는 시장의 변화와 새로운 아이디어를 빠르게 읽는 힘으로 나타나요.",
        "물" to "물 원소는 감정, 직감, 보호의 에너지를 뜻해요. 재물 흐름에서는 분위기와 심리적 위험을 섬세하게 감지하는 힘으로 나타나요."
    )

    private fun WesternZodiacSign.keywordDescription(): String =
        when (this) {
            WesternZodiacSign.ARIES -> "돌파력은 망설임보다 실행을 먼저 선택하는 힘이에요. 투자에서는 기회 포착과 빠른 진입 판단으로 나타나요."
            WesternZodiacSign.TAURUS -> "안정성은 흔들림 속에서도 기준을 지키는 힘이에요. 투자에서는 검증된 가치와 꾸준한 축적을 선호하는 감각으로 나타나요."
            WesternZodiacSign.GEMINI -> "기민함은 정보 변화에 빠르게 반응하는 힘이에요. 투자에서는 여러 뉴스와 데이터를 비교하며 방향을 바꾸는 감각으로 나타나요."
            WesternZodiacSign.CANCER -> "보호본능은 내 생활 기반을 먼저 지키려는 힘이에요. 투자에서는 손실 가능성과 심리적 안전지대를 먼저 살피는 태도로 나타나요."
            WesternZodiacSign.LEO -> "표현력은 확신을 밖으로 드러내고 주도하려는 힘이에요. 투자에서는 큰 흐름을 잡았을 때 과감하게 비중을 실으려는 성향으로 나타나요."
            WesternZodiacSign.VIRGO -> "정리력은 복잡한 정보를 기준에 맞게 나누는 힘이에요. 투자에서는 숫자, 조건, 리스크를 꼼꼼히 확인하는 습관으로 나타나요."
            WesternZodiacSign.LIBRA -> "균형감은 치우친 판단을 조율하는 힘이에요. 투자에서는 비중, 타이밍, 의견을 비교하며 무리 없는 선택을 찾는 감각으로 나타나요."
            WesternZodiacSign.SCORPIO -> "집중력은 표면 아래의 구조까지 파고드는 힘이에요. 투자에서는 한 자산이나 테마를 깊게 분석하는 몰입으로 나타나요."
            WesternZodiacSign.SAGITTARIUS -> "확장성은 지금보다 더 큰 가능성을 보려는 힘이에요. 투자에서는 성장 산업과 장기 스토리를 넓은 시야로 읽는 감각으로 나타나요."
            WesternZodiacSign.CAPRICORN -> "현실감각은 목표와 지속 가능성을 먼저 따지는 힘이에요. 투자에서는 장기 계획, 원칙, 감당 가능한 위험을 중시하는 태도로 나타나요."
            WesternZodiacSign.AQUARIUS -> "관점전환은 익숙한 해석에서 벗어나 새 구조를 보는 힘이에요. 투자에서는 혁신 산업이나 시장 변화의 단서를 찾는 감각으로 나타나요."
            WesternZodiacSign.PISCES -> "직감은 말로 설명되기 전의 분위기를 읽는 힘이에요. 투자에서는 시장 심리와 과열, 불안을 섬세하게 감지하는 감각으로 나타나요."
        }

    private fun WesternZodiacSign.profileSummary(): String =
        when (this) {
            WesternZodiacSign.ARIES -> "양자리는 기회가 보이면 먼저 움직이려는 힘이 강해요. 재물 운에서는 빠른 판단과 실행력이 장점이지만, 분위기에 올라타는 속도가 너무 빠르면 충동 매수로 이어질 수 있어요. 진입 전 손절 기준과 투자 금액 한도를 정하면 돌파력이 안정적인 성과로 연결돼요."
            WesternZodiacSign.TAURUS -> "황소자리는 안정감과 실속을 중요하게 보는 별자리예요. 재물 운에서는 검증된 가치와 장기적인 축적에 강하지만, 익숙한 선택을 오래 붙잡아 변화 신호를 늦게 볼 수 있어요. 정기적으로 보유 이유를 점검하면 꾸준함이 더 건강한 자산 관리로 이어져요."
            WesternZodiacSign.GEMINI -> "쌍둥이자리는 정보 흐름을 빠르게 읽고 관점을 바꾸는 힘이 좋아요. 재물 운에서는 새로운 테마와 뉴스에 민감하게 반응하지만, 기준이 자주 바뀌면 잦은 매매로 피로가 쌓일 수 있어요. 관심 종목을 좁히고 같은 체크리스트로 비교하면 기민함이 강점으로 남아요."
            WesternZodiacSign.CANCER -> "게자리는 심리적 안정과 보호 본능이 강한 별자리예요. 재물 운에서는 위험을 빨리 감지하고 생활 기반을 지키는 데 강하지만, 불안한 날에는 시장 소음에도 흔들릴 수 있어요. 투자금과 생활비를 분리하면 안정감 속에서 더 차분하게 판단할 수 있어요."
            WesternZodiacSign.LEO -> "사자자리는 확신이 생기면 크게 밀고 나가는 힘이 있어요. 재물 운에서는 성장성과 주도 흐름을 읽는 데 강하지만, 자존심이 개입되면 틀렸다는 신호를 늦게 인정할 수 있어요. 매수 이유만큼 매도 조건을 선명히 적어두면 자신감이 무리한 리스크로 번지는 것을 줄일 수 있어요."
            WesternZodiacSign.VIRGO -> "처녀자리는 작은 신호를 세밀하게 읽고 정돈된 판단을 선호해요. 재물 운에서는 숫자와 조건을 꼼꼼히 확인하는 힘이 장점이지만, 완벽한 타이밍을 기다리다 기회를 놓칠 수 있어요. 분석 시간을 제한하고 작은 금액으로 검증하면 신중함과 실행 사이의 균형이 좋아져요."
            WesternZodiacSign.LIBRA -> "천칭자리는 여러 선택지 사이에서 균형점을 찾는 힘이 좋아요. 재물 운에서는 분산과 비중 조절에 강하지만, 의견을 너무 많이 비교하면 결정이 늦어질 수 있어요. 비교 대상을 줄이고 최종 판단 기준을 하나 정하면 균형감이 더 실용적으로 작동해요."
            WesternZodiacSign.SCORPIO -> "전갈자리는 겉보다 속을 읽고 깊게 파고드는 힘이 강해요. 재물 운에서는 숨은 리스크와 구조를 보는 데 강하지만, 한 자산에 과몰입하면 손실 회복 집착으로 이어질 수 있어요. 확신이 큰 자산일수록 최대 비중과 휴식 규칙을 정해두는 편이 좋아요."
            WesternZodiacSign.SAGITTARIUS -> "사수자리는 큰 흐름과 미래 가능성을 넓게 보는 별자리예요. 재물 운에서는 성장 산업과 장기 스토리를 읽는 데 강하지만, 기대가 앞서면 세부 리스크를 가볍게 볼 수 있어요. 낙관, 중립, 비관 시나리오를 함께 적으면 확장성이 더 균형 있게 쓰여요."
            WesternZodiacSign.CAPRICORN -> "염소자리는 현실적인 목표와 지속 가능성을 먼저 보는 별자리예요. 재물 운에서는 장기 계획과 원칙 매매에 강하지만, 지나치게 보수적이면 성장 기회를 놓칠 수 있어요. 안정 자산을 중심에 두되 작은 성장 비중을 열어두면 현실감각이 더 넓게 작동해요."
            WesternZodiacSign.AQUARIUS -> "물병자리는 익숙한 방식에서 벗어나 새로운 구조를 보는 힘이 있어요. 재물 운에서는 혁신 산업과 시장 변화의 단서를 잘 찾지만, 아이디어가 실제 수익성보다 앞설 수 있어요. 새로운 테마는 작은 비중으로 검증하고 숫자로 확인되는 지표를 붙이면 안정감이 생겨요."
            WesternZodiacSign.PISCES -> "물고기자리는 분위기와 감정의 흐름을 섬세하게 읽는 별자리예요. 재물 운에서는 시장 심리와 과열 신호를 감각적으로 포착하지만, 직감만으로 움직이면 기준이 흐려질 수 있어요. 매수 전 가격, 기간, 손실 한도를 숫자로 적으면 감각과 현실의 균형을 잡기 쉬워요."
        }

    private fun WesternZodiacSign.traitDetails(): List<ZodiacTraitResponse> =
        when (this) {
            WesternZodiacSign.ARIES -> listOf(
                ZodiacTraitResponse("직진성", "기회가 보이면 망설임 없이 움직이는 성향이에요. 빠른 시장에서는 장점이지만, 확인 절차가 부족하면 과열 구간에 들어갈 수 있어요."),
                ZodiacTraitResponse("결단력", "선택지를 오래 끌지 않고 방향을 정하는 힘이에요. 투자에서는 진입과 청산 기준을 미리 세울수록 더 안정적으로 발휘돼요."),
                ZodiacTraitResponse("생동감", "새로운 흐름에서 에너지를 얻는 특징이에요. 다만 자극적인 이슈에만 반응하지 않도록 기본 데이터를 함께 보는 게 좋아요.")
            )
            WesternZodiacSign.TAURUS -> listOf(
                ZodiacTraitResponse("안정감", "변동성 속에서도 쉽게 흔들리지 않는 성향이에요. 장기 보유에는 유리하지만 변화 신호를 정기적으로 점검해야 해요."),
                ZodiacTraitResponse("인내심", "시간을 들여 결과를 기다리는 힘이에요. 분할 매수와 적립식 전략처럼 꾸준함이 필요한 방식과 잘 맞아요."),
                ZodiacTraitResponse("실리성", "화려한 이야기보다 실제 가치와 효용을 따지는 특징이에요. 수익성, 배당, 현금흐름처럼 확인 가능한 기준을 볼 때 강점이 살아나요.")
            )
            WesternZodiacSign.GEMINI -> listOf(
                ZodiacTraitResponse("호기심", "새로운 정보와 테마를 빠르게 탐색하는 성향이에요. 관심 범위가 넓어질수록 핵심 기준을 정해두는 게 필요해요."),
                ZodiacTraitResponse("순발력", "변화가 생겼을 때 빠르게 반응하는 힘이에요. 단기 이슈 대응에는 좋지만 잦은 매매로 이어지지 않게 기록이 필요해요."),
                ZodiacTraitResponse("유연함", "한 가지 관점에 갇히지 않고 해석을 바꾸는 특징이에요. 다만 기준 없는 전환은 흔들림이 될 수 있어요.")
            )
            WesternZodiacSign.CANCER -> listOf(
                ZodiacTraitResponse("공감력", "사람들의 불안과 기대를 섬세하게 읽는 성향이에요. 시장 심리를 파악하는 데 도움이 되지만 주변 의견에 휩쓸리지 않는 기준이 필요해요."),
                ZodiacTraitResponse("보호성", "내 자산과 생활 기반을 먼저 지키려는 특징이에요. 비상금과 방어 자산 관리에서 강점이 살아나요."),
                ZodiacTraitResponse("섬세함", "작은 변화에도 민감하게 반응하는 힘이에요. 불안한 날에는 전체 포트폴리오 기준으로 상황을 보는 습관이 좋아요.")
            )
            WesternZodiacSign.LEO -> listOf(
                ZodiacTraitResponse("자신감", "확신이 생기면 주저하지 않고 선택하는 성향이에요. 반대 근거를 함께 확인하면 과신을 줄일 수 있어요."),
                ZodiacTraitResponse("리더십", "흐름을 주도하고 큰 방향을 잡는 힘이에요. 주도 섹터를 볼 때 강하지만 비중 관리가 함께 필요해요."),
                ZodiacTraitResponse("따뜻함", "사람과 브랜드의 매력을 크게 보는 특징이에요. 스토리뿐 아니라 실적과 가격도 함께 확인하면 판단이 탄탄해져요.")
            )
            WesternZodiacSign.VIRGO -> listOf(
                ZodiacTraitResponse("분석력", "정보를 세부 항목으로 나눠 검토하는 힘이에요. 재무제표와 리스크 체크에서 강점이 커요."),
                ZodiacTraitResponse("성실함", "정한 루틴을 꾸준히 지키는 성향이에요. 정기 리밸런싱과 기록 관리에 잘 맞아요."),
                ZodiacTraitResponse("정돈감", "복잡한 상황을 체계적으로 정리하는 특징이에요. 다만 모든 조건을 완벽히 맞추려 하면 실행이 늦어질 수 있어요.")
            )
            WesternZodiacSign.LIBRA -> listOf(
                ZodiacTraitResponse("균형감", "한쪽으로 치우치지 않고 중간 지점을 찾는 힘이에요. 분산 투자와 비중 조절에서 장점이 돋보여요."),
                ZodiacTraitResponse("관계감각", "다른 사람의 의견과 분위기를 잘 읽는 특징이에요. 참고는 좋지만 최종 판단 기준은 스스로 정해야 해요."),
                ZodiacTraitResponse("세련됨", "무리한 선택보다 조화로운 구성을 선호하는 성향이에요. 포트폴리오 전체의 모양을 볼 때 강점이 있어요.")
            )
            WesternZodiacSign.SCORPIO -> listOf(
                ZodiacTraitResponse("집중력", "한 가지 주제를 깊게 파고드는 힘이에요. 심층 분석에는 강하지만 과몰입을 막는 비중 제한이 필요해요."),
                ZodiacTraitResponse("통찰력", "표면적인 가격보다 숨은 구조를 보려는 특징이에요. 리스크와 기회를 함께 파악할 때 장점이 커요."),
                ZodiacTraitResponse("몰입감", "확신한 대상에 에너지를 집중하는 성향이에요. 휴식 규칙을 두면 판단 피로를 줄일 수 있어요.")
            )
            WesternZodiacSign.SAGITTARIUS -> listOf(
                ZodiacTraitResponse("낙관성", "미래 가능성을 긍정적으로 보는 힘이에요. 성장 자산에는 잘 맞지만 비관 시나리오도 함께 확인해야 해요."),
                ZodiacTraitResponse("확장성", "넓은 시장과 새로운 기회를 탐색하는 특징이에요. 해외, 신산업, 장기 테마를 볼 때 강점이 있어요."),
                ZodiacTraitResponse("직관력", "큰 방향을 빠르게 감지하는 성향이에요. 세부 숫자로 보완하면 판단의 안정성이 좋아져요.")
            )
            WesternZodiacSign.CAPRICORN -> listOf(
                ZodiacTraitResponse("책임감", "목표와 결과를 무겁게 받아들이는 성향이에요. 장기 계획에는 강하지만 단기 성과 압박은 줄이는 편이 좋아요."),
                ZodiacTraitResponse("현실감", "감정보다 실제 조건과 지속 가능성을 보는 힘이에요. 원칙 기반 투자와 잘 맞아요."),
                ZodiacTraitResponse("지구력", "오래 버티며 계획을 이어가는 특징이에요. 다만 변화가 큰 시장에서는 조정 규칙도 함께 필요해요.")
            )
            WesternZodiacSign.AQUARIUS -> listOf(
                ZodiacTraitResponse("독창성", "남들이 보지 못한 관점에서 가능성을 찾는 힘이에요. 혁신 테마에는 강하지만 실제 수익성 검증이 필요해요."),
                ZodiacTraitResponse("객관성", "감정에서 거리를 두고 구조를 보려는 특징이에요. 포트폴리오를 점검할 때 장점이 커요."),
                ZodiacTraitResponse("유연함", "새로운 방식과 변화를 받아들이는 성향이에요. 다만 너무 앞선 아이디어에는 작은 비중으로 접근하는 편이 좋아요.")
            )
            WesternZodiacSign.PISCES -> listOf(
                ZodiacTraitResponse("직감", "말로 설명하기 전의 분위기를 읽는 힘이에요. 시장 심리 파악에는 좋지만 숫자 기준으로 보완해야 해요."),
                ZodiacTraitResponse("감수성", "작은 감정 변화와 불안을 섬세하게 느끼는 특징이에요. 투자 판단 전 컨디션을 확인하면 흔들림을 줄일 수 있어요."),
                ZodiacTraitResponse("공감력", "사람들의 기대와 공포를 잘 받아들이는 성향이에요. 군중 심리를 읽는 데 도움이 되지만 그 안에 휩쓸리지 않는 기준이 필요해요.")
            )
        }

    private val zodiacProfileDetails = mapOf(
        WesternZodiacSign.ARIES to ZodiacProfileDetail(
            strengths = listOf("기회 포착이 빠름", "결정 후 실행력이 좋음", "분위기 전환에 강함"),
            cautions = listOf("충동 매수", "손실을 만회하려는 과속", "검증 전 선점 욕구"),
            moneyStyle = "흐름이 보이면 먼저 움직이며, 짧은 타이밍과 명확한 승부처에서 에너지가 살아나요.",
            investmentTendency = "상승 모멘텀에는 강하지만 진입 전 손절 기준과 투자 금액 한도를 먼저 정해야 안정적이에요.",
            careTip = "결정 직전 10분만 늦추고, 왜 지금이어야 하는지 한 문장으로 적어보면 과열을 줄일 수 있어요."
        ),
        WesternZodiacSign.TAURUS to ZodiacProfileDetail(
            strengths = listOf("꾸준한 축적", "가치 판단의 안정감", "변동성에 쉽게 흔들리지 않음"),
            cautions = listOf("익숙한 선택 고집", "손실 포지션 방치", "변화 신호를 늦게 반영"),
            moneyStyle = "검증된 자산과 예측 가능한 흐름을 선호하며, 시간을 들여 쌓는 방식에서 편안함을 느껴요.",
            investmentTendency = "장기 보유와 분산에는 강점이 있지만, 정기적으로 전제 조건이 바뀌었는지 확인하는 루틴이 필요해요.",
            careTip = "한 달에 한 번은 보유 이유를 다시 적고, 이유가 사라진 자산은 비중 조정을 검토해보세요."
        ),
        WesternZodiacSign.GEMINI to ZodiacProfileDetail(
            strengths = listOf("정보 수집 속도", "관점 전환", "새로운 테마 이해력"),
            cautions = listOf("정보 과다로 인한 흔들림", "잦은 갈아타기", "깊이보다 속도에 치우침"),
            moneyStyle = "뉴스와 데이터 흐름에 민감하고, 여러 가능성을 비교하면서 판단하는 스타일이에요.",
            investmentTendency = "테마 탐색과 분할 접근에 강하지만, 매매 기준이 자주 바뀌면 수익률 관리가 어려워질 수 있어요.",
            careTip = "관심 종목을 늘리기보다 핵심 체크리스트 3개를 정해 같은 기준으로 비교해보세요."
        ),
        WesternZodiacSign.CANCER to ZodiacProfileDetail(
            strengths = listOf("위험 감지", "방어적 자금 관리", "생활 안정 우선"),
            cautions = listOf("불안에 따른 성급한 매도", "주변 분위기에 영향받음", "현금 비중 과잉"),
            moneyStyle = "심리적 안정과 가족, 생활 기반을 중요하게 보며 안전한 울타리가 있을 때 판단력이 좋아져요.",
            investmentTendency = "방어 자산과 비상금 관리에는 강하지만, 불안한 날에는 시장 소음과 실제 위험을 구분해야 해요.",
            careTip = "투자금과 생활비 계좌를 분리하고, 불안할 때는 포트폴리오 전체 손실률부터 확인하세요."
        ),
        WesternZodiacSign.LEO to ZodiacProfileDetail(
            strengths = listOf("확신 있는 선택", "큰 흐름을 보는 힘", "리더십 있는 결단"),
            cautions = listOf("자존심 매매", "수익 과시 후 리스크 확대", "틀렸다는 신호를 늦게 인정"),
            moneyStyle = "확신이 생기면 크게 밀고 나가며, 성장성과 존재감이 뚜렷한 선택에 끌리는 편이에요.",
            investmentTendency = "주도주나 성장 스토리 파악에 강하지만, 확신이 강할수록 반대 근거를 함께 확인해야 해요.",
            careTip = "매수 이유만큼 매도 조건을 선명하게 적어두면 자존심과 판단을 분리하기 쉬워요."
        ),
        WesternZodiacSign.VIRGO to ZodiacProfileDetail(
            strengths = listOf("분석력", "리스크 점검", "세부 조건 확인"),
            cautions = listOf("완벽한 타이밍 집착", "과도한 걱정", "작은 오차에 따른 결정 지연"),
            moneyStyle = "숫자와 근거를 꼼꼼히 확인하고, 구조가 정돈되어 있을수록 안정적으로 움직여요.",
            investmentTendency = "재무제표, 비용, 리스크 관리에 강하지만 모든 변수를 통제하려 하면 기회를 놓칠 수 있어요.",
            careTip = "분석 시간을 제한하고, 기준을 충족하면 작은 금액으로 먼저 실행하는 연습이 도움이 돼요."
        ),
        WesternZodiacSign.LIBRA to ZodiacProfileDetail(
            strengths = listOf("균형 잡힌 비교", "분산 감각", "과열된 분위기 조율"),
            cautions = listOf("결정 지연", "타인의 의견에 흔들림", "무난함만 좇는 선택"),
            moneyStyle = "여러 선택지를 비교하며 균형점을 찾고, 포트폴리오의 조화를 중요하게 여겨요.",
            investmentTendency = "분산과 비중 조절에는 강하지만, 모두의 의견을 기다리면 진입과 청산이 늦어질 수 있어요.",
            careTip = "비교 대상은 3개로 제한하고, 최종 판단 기준 하나를 미리 정해두세요."
        ),
        WesternZodiacSign.SCORPIO to ZodiacProfileDetail(
            strengths = listOf("집중력", "숨은 리스크 탐지", "깊은 탐구"),
            cautions = listOf("한 자산에 과몰입", "손실 회복 집착", "의심으로 인한 피로"),
            moneyStyle = "겉으로 보이는 가격보다 안쪽의 구조와 심리를 파고들며, 확신한 대상에는 깊게 몰입해요.",
            investmentTendency = "심층 분석과 역발상에는 강하지만, 몰입도가 높을수록 비중 제한과 휴식 규칙이 필요해요.",
            careTip = "가장 확신하는 자산일수록 최대 비중을 정하고, 판단이 예민한 날에는 추가 매수를 미뤄보세요."
        ),
        WesternZodiacSign.SAGITTARIUS to ZodiacProfileDetail(
            strengths = listOf("큰 흐름 파악", "미래 성장성 감지", "낙관적 회복력"),
            cautions = listOf("근거보다 기대가 앞섬", "해외/신산업 테마 과몰입", "세부 리스크 간과"),
            moneyStyle = "넓은 시장과 새로운 가능성에 끌리며, 장기 성장 스토리를 볼 때 판단이 활발해져요.",
            investmentTendency = "성장 섹터와 글로벌 흐름을 읽는 데 강하지만, 기대 수익만큼 하락 시나리오도 함께 봐야 해요.",
            careTip = "투자 아이디어마다 낙관, 중립, 비관 시나리오를 하나씩 적어 균형을 맞춰보세요."
        ),
        WesternZodiacSign.CAPRICORN to ZodiacProfileDetail(
            strengths = listOf("현실적 판단", "장기 계획", "자기 통제"),
            cautions = listOf("보수성 과잉", "성과 압박", "실패를 오래 붙잡음"),
            moneyStyle = "수익보다 지속 가능성을 먼저 보며, 목표와 계획이 분명할 때 가장 안정적으로 움직여요.",
            investmentTendency = "장기 포트폴리오와 원칙 매매에 강하지만, 지나치게 보수적이면 성장 기회를 놓칠 수 있어요.",
            careTip = "안정 자산 안에서도 성장 비중을 작게 열어두고, 성과 평가는 짧게 끊어 보지 않는 편이 좋아요."
        ),
        WesternZodiacSign.AQUARIUS to ZodiacProfileDetail(
            strengths = listOf("독창적 관점", "구조 변화 감지", "객관적 거리두기"),
            cautions = listOf("너무 앞선 테마 선택", "현실 수익성 간과", "변동성에 대한 과소평가"),
            moneyStyle = "기존 질서가 바뀌는 지점에 민감하고, 남들이 보지 못한 가능성을 찾는 데 흥미를 느껴요.",
            investmentTendency = "혁신 산업과 새로운 시장 구조를 읽는 데 강하지만, 아이디어와 실제 실적을 분리해 봐야 해요.",
            careTip = "새로운 테마는 작은 비중으로 검증하고, 숫자로 확인되는 지표를 최소 하나 붙여두세요."
        ),
        WesternZodiacSign.PISCES to ZodiacProfileDetail(
            strengths = listOf("직감적 분위기 감지", "공감 기반 판단", "유연한 수용력"),
            cautions = listOf("분위기에 휩쓸림", "경계 없는 손실 감수", "현실 점검 부족"),
            moneyStyle = "시장 분위기와 사람들의 감정을 섬세하게 읽고, 흐름의 온도를 감각적으로 받아들여요.",
            investmentTendency = "심리 흐름을 읽는 데 강하지만, 직감만으로 움직이면 기준이 흐려질 수 있어요.",
            careTip = "매수 전 가격, 기간, 손실 한도를 숫자로 적어두면 감각과 현실의 균형을 잡기 쉬워요."
        )
    )

    private fun buildBirthTarot(
        cardCode: String,
        preferredDeckVersionId: String?
    ): BirthTarotResponse {
        val canonicalCard = TarotCard.fromCode(cardCode)
        val deckVersionId = resolveBirthTarotDeckVersionId(preferredDeckVersionId)
        val card = tarotCardMetadataRepository.findByDeckVersion_IdAndCode(
            deckVersionId = deckVersionId,
            code = canonicalCard.code
        ) ?: throw ResponseStatusException(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "birth tarot metadata not found for deckVersionId=$deckVersionId, code=${canonicalCard.code}"
        )
        val birthInterpretation = tarotBirthCardRepository.findByCardSetIdAndCode(
            cardSetId = card.cardSetId,
            code = canonicalCard.code
        )?.toInterpretation() ?: throw ResponseStatusException(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "birth tarot interpretation not found for cardSetId=${card.cardSetId}, code=${canonicalCard.code}"
        )

        return card.toBirthTarotResponse(
            number = canonicalCard.cardNumber,
            birthInterpretation = birthInterpretation
        )
    }

    private fun resolveBirthTarotDeckVersionId(preferredDeckVersionId: String?): String {
        tarotDeckVersionRepository.findFirstByActiveTrueAndDeckRoleOrderByDisplayOrderAscNameAsc(TarotDeckRole.MAIN)
            ?.let { return it.id }

        val candidateId = preferredDeckVersionId?.trim()?.ifBlank { null } ?: DEFAULT_TAROT_DECK_VERSION_ID
        val deck = tarotDeckVersionRepository.findById(candidateId).orElse(null)
            ?: return DEFAULT_TAROT_DECK_VERSION_ID
        if (!deck.active || deck.deckRole != TarotDeckRole.MAIN) {
            return DEFAULT_TAROT_DECK_VERSION_ID
        }
        return deck.id
    }

    private fun buildSajuProfile(
        consultingResult: com.hwcompany.fortune_index.saju.SajuConsultingResult
    ): SajuProfileResponse {
        val natalChart = consultingResult.analysis.natalChart
        val dayMasterStem = natalChart.day.heavenlyStem

        return SajuProfileResponse(
            palza = listOf(
                natalChart.year.toHanjaString(),
                natalChart.month.toHanjaString(),
                natalChart.day.toHanjaString(),
                natalChart.hour.toHanjaString()
            ),
            ohang = consultingResult.analysis.fiveElementBalance.toResponse(),
            ilju = buildDayPillarInsight(
                dayPillar = natalChart.day,
                dayMaster = consultingResult.dayMaster,
                dayBranch = consultingResult.dayBranch,
                dayBranchTenStar = sajuAnalyzer.calculateTenStar(dayMasterStem, natalChart.day.earthlyBranch)
            ),
            wolji = buildMonthBranchInsight(
                monthBranch = natalChart.month.earthlyBranch,
                monthBranchEnergy = consultingResult.monthBranch,
                monthBranchTenStar = sajuAnalyzer.calculateTenStar(dayMasterStem, natalChart.month.earthlyBranch)
            ),
            daeun = consultingResult.currentFortune.majorFortune.toInsight(),
            sewun = consultingResult.currentFortune.toYearlyInsight()
        )
    }

    private fun com.hwcompany.fortune_index.domain.model.FiveElementsProfile.toPercentages(): List<Int> {
        val values = listOf(
            wood,
            fire,
            earth,
            metal,
            water
        )
        val total = values.fold(BigDecimal.ZERO, BigDecimal::add)
        if (total.compareTo(BigDecimal.ZERO) <= 0) {
            return listOf(0, 0, 0, 0, 0)
        }

        val scaled = values.map {
            it.multiply(HUNDRED).divide(total, 0, RoundingMode.DOWN).toInt()
        }.toMutableList()
        val remainder = 100 - scaled.sum()
        scaled[scaled.indices.maxBy { values[it] }] += remainder
        return scaled
    }

    private fun com.hwcompany.fortune_index.domain.model.FiveElementsProfile.toResponse(): SajuOhangResponse {
        val percentages = toPercentages()
        return SajuOhangResponse(
            wood = percentages[0],
            fire = percentages[1],
            earth = percentages[2],
            metal = percentages[3],
            water = percentages[4]
        )
    }

    private fun FiveElementBalance.toResponse(): SajuOhangResponse =
        SajuOhangResponse(
            wood = wood,
            fire = fire,
            earth = earth,
            metal = metal,
            water = water
        )

    private fun buildDayPillarInsight(
        dayPillar: Pillar,
        dayMaster: SajuCoreEnergy,
        dayBranch: SajuCoreEnergy,
        dayBranchTenStar: TenStar
    ): SajuInsightResponse {
        val ganji = SajuGanji.of(dayPillar.heavenlyStem, dayPillar.earthlyBranch.toZodiac())
        val dayPillarSummary = sajuInterpretationService.getInterpretation(
            SajuInterpretationCategory.DAY_PILLAR,
            ganji.code
        )?.summaryEasy ?: (
            "${dayPillar.toKoreanString()} 일주는 나를 가장 잘 보여주는 기둥이에요. " +
                "${dayMaster.toSimpleImage()}처럼 기본 마음은 ${dayMaster.toSimpleTrait()} 편이고, " +
                "${dayBranch.toSimpleImage()} 기운이 함께 있어 ${dayBranch.toSimpleTrait()} 모습도 같이 보여요."
            )
        val tenStarSummary = sajuInterpretationService.getInterpretation(
            SajuInterpretationCategory.TEN_STAR,
            dayBranchTenStar.name
        )?.summaryEasy ?: dayBranchTenStar.toSimpleMeaning()

        return SajuInsightResponse(
            name = "${dayPillar.toKoreanString()} (${dayPillar.toHanjaString()})",
            summary = "$dayPillarSummary 일지의 힘은 ${dayBranchTenStar.labelKo()}이라 $tenStarSummary"
        )
    }

    private fun buildMonthBranchInsight(
        monthBranch: EarthlyBranch,
        monthBranchEnergy: SajuCoreEnergy,
        monthBranchTenStar: TenStar
    ): SajuInsightResponse {
        val monthBranchSummary = sajuInterpretationService.getInterpretation(
            SajuInterpretationCategory.MONTH_BRANCH,
            monthBranch.name
        )?.summaryEasy ?: (
            "${monthBranch.labelKo()}는 ${monthBranchEnergy.toSimpleImage()} 기운이라 " +
                "${monthBranchEnergy.toSimpleTrait()} 분위기 속에서 힘을 쓰기 쉬워요."
            )
        val tenStarSummary = sajuInterpretationService.getInterpretation(
            SajuInterpretationCategory.TEN_STAR,
            monthBranchTenStar.name
        )?.summaryEasy ?: monthBranchTenStar.toSimpleMeaning()

        return SajuInsightResponse(
            name = "${monthBranch.labelKo()} 월지 (${monthBranch.toHanja()})",
            summary = "$monthBranchSummary 월지의 힘은 ${monthBranchTenStar.labelKo()}이라 $tenStarSummary"
        )
    }

    private fun com.hwcompany.fortune_index.saju.MajorFortuneDto.toInsight(): FortuneInsightResponse {
        val template = sajuInterpretationService.getInterpretation(
            SajuInterpretationCategory.FORTUNE_TYPE,
            "MAJOR"
        )?.summaryEasy ?: "지금은 {stemSummary} {branchSummary}"
        return FortuneInsightResponse(
            name = "${startAge}-${endAge}세 ${pillar.toKoreanString()} (${pillar.toHanjaString()})",
            summary = template
                .replace("{stemSummary}", stemTenStarSummary(stemTenStar))
                .replace("{branchSummary}", branchTenStarSummary(branchTenStar))
        )
    }

    private fun com.hwcompany.fortune_index.saju.CurrentFortuneDto.toYearlyInsight(): FortuneInsightResponse {
        val template = sajuInterpretationService.getInterpretation(
            SajuInterpretationCategory.FORTUNE_TYPE,
            "YEARLY"
        )?.summaryEasy ?: "올해는 {stemSummary} {branchSummary}"
        return FortuneInsightResponse(
            name = "${referenceYear}년 ${yearlyFortune.pillar.toKoreanString()} (${yearlyFortune.pillar.toHanjaString()})",
            summary = template
                .replace("{stemSummary}", stemTenStarSummary(yearlyFortune.stemTenStar))
                .replace("{branchSummary}", branchTenStarSummary(yearlyFortune.branchTenStar))
        )
    }

    private fun Pillar.toHanjaString(): String = heavenlyStem.toHanja() + earthlyBranch.toHanja()

    private fun Pillar.toKoreanString(): String = heavenlyStem.labelKo() + earthlyBranch.labelKo()

    private fun HeavenlyStem.toHanja(): String =
        when (this) {
            HeavenlyStem.GAP -> "甲"
            HeavenlyStem.EUL -> "乙"
            HeavenlyStem.BYEONG -> "丙"
            HeavenlyStem.JEONG -> "丁"
            HeavenlyStem.MU -> "戊"
            HeavenlyStem.GI -> "己"
            HeavenlyStem.GYEONG -> "庚"
            HeavenlyStem.SIN -> "辛"
            HeavenlyStem.IM -> "壬"
            HeavenlyStem.GYE -> "癸"
        }

    private fun EarthlyBranch.toHanja(): String =
        when (this) {
            EarthlyBranch.JA -> "子"
            EarthlyBranch.CHUK -> "丑"
            EarthlyBranch.IN -> "寅"
            EarthlyBranch.MYO -> "卯"
            EarthlyBranch.JIN -> "辰"
            EarthlyBranch.SA -> "巳"
            EarthlyBranch.O -> "午"
            EarthlyBranch.MI -> "未"
            EarthlyBranch.SIN -> "申"
            EarthlyBranch.YU -> "酉"
            EarthlyBranch.SUL -> "戌"
            EarthlyBranch.HAE -> "亥"
        }

    private fun TenStar.labelKo(): String =
        when (this) {
            TenStar.BIGYEON -> "비견"
            TenStar.GEOPJAE -> "겁재"
            TenStar.SIKSIN -> "식신"
            TenStar.SANGGWAN -> "상관"
            TenStar.PYEONJAE -> "편재"
            TenStar.JEONGJAE -> "정재"
            TenStar.PYEONGWAN -> "편관"
            TenStar.JEONGGWAN -> "정관"
            TenStar.PYEONIN -> "편인"
            TenStar.JEONGIN -> "정인"
        }

    private fun TenStar.toSimpleMeaning(): String =
        when (this) {
            TenStar.BIGYEON -> "내 힘으로 직접 해보는 일"
            TenStar.GEOPJAE -> "경쟁 속에서 내 몫을 챙기는 일"
            TenStar.SIKSIN -> "재능과 생각을 천천히 꺼내는 일"
            TenStar.SANGGWAN -> "표현이 많아지고 하고 싶은 말이 커지는 일"
            TenStar.PYEONJAE -> "새 기회와 실속을 넓게 보는 일"
            TenStar.JEONGJAE -> "돈과 계획을 차곡차곡 챙기는 일"
            TenStar.PYEONGWAN -> "규칙과 책임을 더 신경 쓰는 일"
            TenStar.JEONGGWAN -> "질서를 잘 지켜 좋은 평가를 받는 일"
            TenStar.PYEONIN -> "새 생각을 배우고 시야를 넓히는 일"
            TenStar.JEONGIN -> "도움받고 배우며 기본기를 쌓는 일"
        }

    private fun SajuCoreEnergy.toSimpleImage(): String =
        when (fiveElement) {
            com.hwcompany.fortune_index.saju.FiveElement.WOOD -> "나무"
            com.hwcompany.fortune_index.saju.FiveElement.FIRE -> "불"
            com.hwcompany.fortune_index.saju.FiveElement.EARTH -> "흙"
            com.hwcompany.fortune_index.saju.FiveElement.METAL -> "쇠"
            com.hwcompany.fortune_index.saju.FiveElement.WATER -> "물"
        }

    private fun SajuCoreEnergy.toSimpleTrait(): String =
        when (fiveElement) {
            com.hwcompany.fortune_index.saju.FiveElement.WOOD -> "자라나듯 천천히 커 가는"
            com.hwcompany.fortune_index.saju.FiveElement.FIRE -> "밝고 힘차게 움직이는"
            com.hwcompany.fortune_index.saju.FiveElement.EARTH -> "차분하고 안정적으로 버티는"
            com.hwcompany.fortune_index.saju.FiveElement.METAL -> "분명하고 단단하게 정리하는"
            com.hwcompany.fortune_index.saju.FiveElement.WATER -> "부드럽고 유연하게 흐르는"
        }

    private fun stemTenStarSummary(tenStar: TenStar): String =
        sajuInterpretationService.getInterpretation(SajuInterpretationCategory.TEN_STAR, tenStar.name)?.summaryEasy
            ?.let { "$it 좋고," }
            ?: "${tenStar.toSimpleMeaning()} 좋고,"

    private fun branchTenStarSummary(tenStar: TenStar): String =
        sajuInterpretationService.getInterpretation(SajuInterpretationCategory.TEN_STAR, tenStar.name)?.summaryEasy
            ?.let { "$it 흐름도 함께 와요." }
            ?: "${tenStar.toSimpleMeaning()} 흐름도 함께 와요."

    private fun EarthlyBranch.toZodiac(): com.hwcompany.fortune_index.common.Zodiac =
        com.hwcompany.fortune_index.common.Zodiac.entries.first { it.branch == this }

    private fun TarotCardMetadataEntity.toBirthTarotResponse(
        number: Int,
        birthInterpretation: TarotBirthCardInterpretation
    ): BirthTarotResponse =
        BirthTarotResponse(
            deckVersionId = deckVersion.id,
            name = name,
            koreanName = koreanName,
            number = number,
            cardMeaning = meaning,
            cardDescription = birthInterpretation.description,
            birthMeaning = birthInterpretation.meaning,
            birthDescription = birthInterpretation.description,
            imageUrl = imageUrl,
            videoUrl = videoUrl
        )

    companion object {
        private val logger = LoggerFactory.getLogger(ProfileDetailsService::class.java)
        private val DEFAULT_ZONE_ID: ZoneId = ZoneId.of("Asia/Seoul")
        private val DEFAULT_BIRTH_TIME: LocalTime = LocalTime.NOON
        private val HUNDRED = BigDecimal("100")
    }
}
