package io.hhplus.tdd;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.PointHistory;
import io.hhplus.tdd.point.PointService;
import io.hhplus.tdd.point.TransactionType;
import io.hhplus.tdd.point.UserPoint;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
public class PointServiceTest {

    @Mock UserPointTable mockPointTable;
    @Mock PointHistoryTable mockHistoryTable;
    @InjectMocks PointService pointService;

    private final long userId = 1L;
    private final long point = 1_000L;
    private final long updateMillis = System.currentTimeMillis();

    //TODO-1. PointService.getPoint() 테스트
    //유저아이디를 사용하여 포인트를 조회한다
    @Test
    void useUserId_getUserPoint() {
        //given
        UserPoint givenUserPoint = new UserPoint(userId, point, updateMillis);
        given(mockPointTable.selectById(userId)).willReturn(givenUserPoint);

        //when
        UserPoint result = pointService.getPoint(userId);

        //then
        assertThat(point).isEqualTo(result.point());
    }

    //TODO-2. PointService.getPointHistory() 테스트
    //유저아이디를 사용하여 포인트사용,충전내역을 조회한 후 모두 반환한다
    @Test
    void useUserId_getUserHistory(){
        //given
        long historyId = 1L;
        PointHistory useHistory = new PointHistory(historyId,userId,point, TransactionType.USE, updateMillis);
        PointHistory chargeHistory = new PointHistory(historyId,userId,point, TransactionType.CHARGE, updateMillis);
        given(mockHistoryTable.selectAllByUserId(userId)).willReturn(List.of(useHistory,chargeHistory));

        //when
        List<PointHistory> result = pointService.getPointHistory(userId);

        //then
        assertThat(result).containsExactly(useHistory,chargeHistory);
    }

    //TODO-3. PointService.charge() 테스트
    //실패1 - 충전금액이 1원 미만일때
    @Test
    void chargeFail_amountLessThanOne(){
        assertThatThrownBy(() -> pointService.charge(userId, -1_000L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("충전금액을 1원이상 입력하세요."); //리뷰포인트1. 검증시 에러메세지 문자열 비교
    }

    //실패2 - 충전시 최대잔고금액기준을 초과할 때
    @Test
    void chargeFail_exceedingMaximumBalance(){
        //given - 해당사용자의 포인트조회시 최대잔고금액인 상태를 설정한다.
        long maxBalance = 999_999L;
        given(mockPointTable.selectById(userId)).willReturn(new UserPoint(userId, maxBalance, updateMillis));

        //when,then
        assertThatThrownBy(() -> pointService.charge(userId, point))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("최대잔고"); //리뷰포인트1. 검증시 에러메세지 문자열 비교
    }

    //충전 성공
    @Test
    void chargeSuccess(){
        //given
        long beforeChargePoint = 5_000L;
        long chargePoint = 1_500L;
        long afterChargePoint = beforeChargePoint+chargePoint;

        //충전 전 포인트내역
        given(mockPointTable.selectById(userId)).willReturn(new UserPoint(userId,beforeChargePoint, updateMillis));
        //충전 후 포인트내역
        given(mockPointTable.insertOrUpdate(userId, afterChargePoint)).willReturn(new UserPoint(userId,afterChargePoint,updateMillis));

        //when
        UserPoint result = pointService.charge(userId,chargePoint);

        //then
        Assertions.assertEquals(afterChargePoint,result.point());
    }

    //TODO-4. PointService.use() 테스트
    //실패1 - 사용금액이 0원 이하일때
    @Test
    void useFail_usingPointLessThanOne(){
        assertThatThrownBy(() -> pointService.use(userId, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("사용금액을 1원이상 입력하세요."); //리뷰포인트1. 검증시 에러메세지 문자열 비교
    }

    //실패2 - 잔액부족
    @Test
    void useFail_balanceNotEnough(){
        //given
        given(mockPointTable.selectById(userId)).willReturn(new UserPoint(userId, 1,updateMillis));

        //when, then
        assertThatThrownBy(() -> pointService.use(userId, 1_000L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("잔액부족"); //리뷰포인트1. 검증시 에러메세지 문자열 비교
    }

    //사용 성공
    @Test
    void useSuccess(){
        //given
        //기존 유저포인트정보
        long beforeUsePoint = 4_000L;   //기존잔액
        long usePoint = 2_500L;         //사용포인트
        long afterUsePoint = beforeUsePoint-usePoint;//사용 후 잔액

        //사용전 유저포인트정보
        given(mockPointTable.selectById(userId)).willReturn(new UserPoint(userId, beforeUsePoint, updateMillis));
        //사용후 유저포인트정보
        given(mockPointTable.insertOrUpdate(userId, afterUsePoint)).willReturn(new UserPoint(userId, afterUsePoint, updateMillis));

        //when
        UserPoint result = pointService.use(userId, usePoint);

        //then - 결과비교
        Assertions.assertEquals(afterUsePoint,result.point());
    }
}
