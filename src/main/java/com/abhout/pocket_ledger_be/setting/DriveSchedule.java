package com.abhout.pocket_ledger_be.setting;

public record DriveSchedule(
    String time, String timezone,
    String cadence
) {}
