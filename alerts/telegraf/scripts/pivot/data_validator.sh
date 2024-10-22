#!/bin/bash

[[ ! -s /gigalogs/dv_for_grafana ]] && exit 1
cat /gigalogs/dv_for_grafana
