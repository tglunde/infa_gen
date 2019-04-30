{% macro columnlist(columns) %}
	{%- for column in columns %}
        SRC.{{column.name}}{% if not loop.last %},{% endif -%}
    {% endfor -%}
{% endmacro %}
{% macro hk(bks) %}
	{%- for column in bks %}
        DECODE(SRC.{{column.name}}, NULL, ''#NULL#'', TO_CHAR(SRC.{{column.name}})){% if not loop.last %}||''|''||{% endif -%}
    {% endfor -%}
{% endmacro %}
{% macro bkstringTGT(bks) %}
	{%- for column in bks %}
        TGT.{{column.name}}{% if not loop.last %},{% endif -%}
    {% endfor -%}
{% endmacro %}
{% macro bkJOIN(bks) %}
	{%- for column in bks %}
        SRC.{{column.name}}=TGT.{{column.name}}{% if not loop.last %} AND {% endif -%}
    {% endfor -%}
{% endmacro %}
call {{subject_area}}_TOOL.LOAD_INSERT('
	WITH SRC as (
    SELECT
          current_timestamp as LDTS
        , ''{{source_name}}'' AS RSRC
        , x''00000000000000000000000000000000'' AS HF
        , {{ columnlist(columns) }}
    FROM {{source_schema}}.{{source_name}}_{{table_name}}_R SRC
	), TGT as (
		select tgt.{{table_name}}_SK SK, {{bkstringTGT(bks)}} from (
			select tgt.*, row_number() over (partition by {{table_name}}_SK order by ldts desc) as version from {{target_schema}}.{{source_name}}_{{table_name}}_HA tgt with ur
		) tgt where version = 1
	)
	SELECT
		SRC.*
	FROM SRC
	LEFT JOIN TGT ON {{bkJOIN(bks)}}
	WHERE 1=1
	AND (
		TGT.SK IS NULL OR (TGT.SK IS NOT NULL AND SRC.HF!=TGT.HF)
	)  WITH UR'
	,
	'{{target_schema}}.{{source_name}}_{{table_name}}_HA');
