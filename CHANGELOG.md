## 1.0.6
- Feature: If the value is invalid-[2147483647 (0x7fffffff)-UNAVAILABLE](https://developer.android.com/reference/android/telephony/CellInfo?_gl=1*1kehidx*_up*MQ..*_ga*MTE1MDI0NjUwMC4xNzY1MjQ2NzY1*_ga_6HH9YJMN9M*czE3NjUyNjE0NTgkbzIkZzAkdDE3NjUyNjE0NTgkajYwJGwwJGgxMTM4NjE2NTM5#UNAVAILABLE), return null.

## 1.0.5
- Feature: Add some parameters(ci,tac,eNB,LCID).

## 1.0.4
- bugfix: 4g EARFCN can return right data.

## 1.0.3
- bugfix: `getAllCellInfoStream()` can return right data.

## 1.0.2
- Support Lte CellInfo via `getCellInfo()` or`getNrOrLteStream()`.
- breaking change: `nrOrLteSignalStream()` change to `allCellInfoStream()`.

## 1.0.1
- Updated LICENSE and README

## 1.0.0

- First stable release.
- Added support for real-time 5G NR info via `requestCellInfoUpdate()`.
- Android only (API 29+).

## 0.0.1

- Initial preview release.

