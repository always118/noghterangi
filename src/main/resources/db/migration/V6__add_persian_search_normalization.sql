-- Query-time normalization covers existing rows and future edits without duplicated search data.
-- Keep the whitespace set aligned with PersianTextNormalizer.
CREATE FUNCTION noghterangi.normalize_persian(value TEXT) RETURNS TEXT
LANGUAGE SQL IMMUTABLE PARALLEL SAFE AS $$
    SELECT btrim(regexp_replace(
        translate(translate(coalesce(value, ''), 'يك', 'یک'),
                  U&'\0009\000a\000b\000c\000d\0020\0085\00a0\1680\2000\2001\2002\2003\2004\2005\2006\2007\2008\2009\200a\200b\200c\200d\2028\2029\202f\205f\3000\feff', repeat(' ', 29)),
        ' +', ' ', 'g'));
$$;
