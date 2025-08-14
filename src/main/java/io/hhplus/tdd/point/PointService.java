package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;

import java.util.List;

public class PointService {
    private final UserPointTable pointTable;
    private final PointHistoryTable pointHistory;

    private static final long MAX_AMOUNT = 1_000_000L;//최대 잔고금액

    public PointService(UserPointTable pointTable, PointHistoryTable pointHistory) {
        this.pointTable = pointTable;
        this.pointHistory = pointHistory;
    }

    //유저의 포인트 조회
    public UserPoint getPoint(long id){
        return pointTable.selectById(id);
    }

    //유저의 포인트 충전/사용내역 조회
    public List<PointHistory> getPointHistory(long id){
        return pointHistory.selectAllByUserId(id);
    }

    //포인트 충전
    public UserPoint charge(long id, long amount){

        //실패1 - 충전금액이 1원 미만일 때
        if(amount <= 0) throw new IllegalArgumentException("충전금액을 1원이상 입력하세요.");

        //사용자의 포인트 조회
        UserPoint userPoint = pointTable.selectById(id);

        //충전 후의 포인트계산
        long chargedPoint = userPoint.point() + amount;

        //실패2 - 충전시 최대잔고금액기준을 초과할 때
        if(chargedPoint > MAX_AMOUNT){
            throw new IllegalArgumentException("최대잔고("+MAX_AMOUNT+"원)를 초과하여 충전할 수 없습니다.");
        }

        //포인트 충전
        UserPoint userPoint_afterCharge = pointTable.insertOrUpdate(id, chargedPoint);

        //포인트 충전내역 저장
        pointHistory.insert(id, amount, TransactionType.CHARGE, System.currentTimeMillis());

        return userPoint_afterCharge;
    }

    //포인트 사용
    public UserPoint use(long id, long amount){

        //실패1 - 사용금액이 0원 이하일 때
        if(amount <= 0) throw new IllegalArgumentException("사용금액을 1원이상 입력하세요.");

        //사용자의 포인트 조회
        UserPoint userPoint = pointTable.selectById(id);

        //현재 포인트
        long currentPoint = userPoint.point();

        //실패2 - 잔액부족
        if(amount > currentPoint) throw new IllegalArgumentException("잔액부족: "+currentPoint+"원");

        //포인트 사용 성공
        UserPoint userPoint_afterUse = pointTable.insertOrUpdate(id, currentPoint-amount);

        //포인트 사용내역 저장
        pointHistory.insert(id, amount, TransactionType.USE, System.currentTimeMillis());

        return userPoint_afterUse;
    }
}
