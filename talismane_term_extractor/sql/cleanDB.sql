--select * from text;
--select * from term;
--select * from term_expansions;
--select * from term_heads;
--select * from context;
--select * from file;

delete from term_expansions;
delete from term_heads;
delete from context;
delete from term;
delete from file;
delete from text;
delete from project;

SELECT setval('public.seq_context_id', 1, true);
SELECT setval('public.seq_term_id', 1, true);
SELECT setval('public.seq_file_id', 1, true);
SELECT setval('public.seq_text_id', 1, true);
SELECT setval('public.seq_project_id', 1, true);
