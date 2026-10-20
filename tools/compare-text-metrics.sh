#!/usr/bin/env bash
set -uo pipefail

if [ "$#" -ne 2 ]; then
    echo "usage: $0 <ours.txt> <golden.txt>" >&2
    exit 2
fi

ours="$1"
golden="$2"

for file in "$ours" "$golden"; do
    if [ ! -s "$file" ]; then
        echo "compare FAILED: $file is missing or empty" >&2
        exit 1
    fi
done

MAX_ADVANCE_DELTA="${RUNTIME_TEXT_ADVANCE_TOLERANCE:-0.05}"
MIN_EXACT="${RUNTIME_TEXT_MIN_EXACT:-870}"

BOUNDS_DELTA="${RUNTIME_TEXT_BOUNDS_TOLERANCE:-1.001}"

awk -v oursFile="$ours" -v maxDelta="$MAX_ADVANCE_DELTA" -v minExact="$MIN_EXACT" -v boundsDelta="$BOUNDS_DELTA" '
function abs(v) { return v < 0 ? -v : v }

function keyOf(line,    quote, head, tail, parts) {
    quote = index(line, "\"")
    if (quote > 0) {
        head = substr(line, 1, quote - 1)
        tail = substr(line, quote)
        split(head, parts, " ")
        return parts[1] " " parts[2] " " parts[3] " " parts[4] " " tail
    }
    split(line, parts, " ")
    return parts[1] " " parts[2] " " parts[3] " " parts[4]
}

function valuesOf(line,    quote, head, parts, count, i, out) {
    quote = index(line, "\"")
    head = quote > 0 ? substr(line, 1, quote - 1) : line
    count = split(head, parts, " ")
    out = ""
    for (i = 5; i <= count; i++) out = out (i > 5 ? " " : "") parts[i]
    return out
}

/^#/ { next }

FILENAME == oursFile {
    mineByKey[keyOf($0)] = valuesOf($0)
    next
}

{
    key = keyOf($0)
    if (!(key in mineByKey)) { missing++; next }

    split(valuesOf($0), device, " ")
    split(mineByKey[key], mine, " ")

    split(key, keyParts, " ")
    kind = keyParts[4]
    compared++

    if (kind == "measure") {
        delta = abs(device[1] - mine[1])
        if (delta > maxDelta) {
            failed++
            if (failed <= 12) printf "  ADVANCE   %-58s device=%-10s ours=%-10s delta=%+.4f\n", key, device[1], mine[1], mine[1] - device[1]
        } else if (delta > 0.05) {
            near++
        } else {
            exact++
        }
    } else if (kind == "metrics") {
        bad = 0
        for (i = 1; i <= 5; i++) if (abs(device[i] - mine[i]) > 0.05) bad = i
        if (bad) {
            failed++
            if (failed <= 12) printf "  METRICS   %-58s field %d device=%s ours=%s\n", key, bad, device[bad], mine[bad]
        } else exact++
    } else if (kind == "metricsInt") {
        bad = 0
        for (i = 1; i <= 5; i++) if (device[i] != mine[i]) bad = i
        if (bad) {
            failed++
            if (failed <= 12) printf "  METRICSINT %-57s field %d device=%s ours=%s\n", key, bad, device[bad], mine[bad]
        } else exact++
    } else if (kind == "widths") {
        deviceCount = 0
        for (i in device) deviceCount++
        mineCount = 0
        for (i in mine) mineCount++

        if (deviceCount != mineCount) {
            failed++
            if (failed <= 12) printf "  WIDTHS    %-58s device has %d values, ours %d\n", key, deviceCount, mineCount
        } else {
            bad = 0
            for (i = 1; i <= deviceCount; i++) if (abs(device[i] - mine[i]) > maxDelta) bad = i
            if (bad) {
                failed++
                if (failed <= 12) printf "  WIDTHS    %-58s glyph %d device=%s ours=%s\n", key, bad, device[bad], mine[bad]
            } else exact++
        }
    } else if (kind == "bounds") {
        bad = 0
        drift = 0
        for (i = 1; i <= 4; i++) {
            if (abs(device[i] - mine[i]) > boundsDelta) bad = i
            else if (device[i] != mine[i]) drift = 1
        }
        if (bad) {
            failed++
            if (failed <= 12) printf "  BOUNDS    %-58s field %d device=%s ours=%s\n", key, bad, device[bad], mine[bad]
        } else if (drift) {
            near++
        } else exact++
    } else {
        skipped++
    }
}

END {
    printf "compared %d rows: %d exact, %d within tolerance, %d failed, %d absent, %d not compared\n",
           compared, exact, near, failed, missing, skipped

    if (exact < minExact) {
        printf "REGRESSION: only %d rows match the device exactly, the committed floor is %d.\n", exact, minExact
        printf "A tolerance nobody can regress under is not a tolerance. Raise the floor only when\n"
        printf "the number genuinely improves, never to make a failure go away.\n"
        exit 1
    }

    exit failed > 0 ? 1 : 0
}
' "$ours" "$golden"

status=$?
if [ "$status" -ne 0 ]; then
    echo "compare FAILED: our metrics diverge from the device golden beyond tolerance" >&2
    exit 1
fi

echo "compare PASSED: text metrics agree with the device within tolerance"
