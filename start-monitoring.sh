#!/usr/bin/env zsh

docker run --name=prometheus -p 9090:9090 -v $(pwd)/monitoring/prometheus.yml:/etc/prometheus/prometheus.yml -d prom/prometheus
docker run --name=grafana -p 3000:3000 \
  --env GF_FEATURE_TOGGLES_ENABLE=publicDashboards \
  -v $(pwd)/monitoring/grafana-datasources.yml:/etc/grafana/provisioning/datasources/automatic.yml \
  -v $(pwd)/monitoring/grafana-dashboard-scheduler.json:/etc/dashboards/scheduler/scheduler_dashboard.json \
  -v $(pwd)/monitoring/grafana-dashboards.yml:/etc/grafana/provisioning/dashboards/automatic.yml \
  -d grafana/grafana

open 'http://localhost:3000/d/b4dbe92d-ed5c-4859-af53-368efc9823f9/task-scheduler-app?orgId=1&refresh=5s'