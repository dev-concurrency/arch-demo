
from grafanalib.core import (
    Dashboard, TimeSeries, GaugePanel,
    Target, GridPos,
    OPS_FORMAT
)

dashboard = Dashboard(
    title="Example dashboard",
    description="Example dashboard using default Prometheus datasource",
    tags=[
        'example'
    ],
    timezone="browser",
    panels=[
        TimeSeries(
            title="Transfers http requests",
            dataSource='default',
            targets=[
                Target(
                    expr='rate(http_requests_total{handler="/payments/transfer",method="POST"}[30s])',
                    refId='A',
                ),
            ],
            unit=OPS_FORMAT,
            gridPos=GridPos(h=8, w=16, x=0, y=10),
        ),
        TimeSeries(
            title="Transfers http requests response times",
            dataSource='default',
            targets=[
                Target(
                    expr='rate(http_request_duration_seconds_sum{handler="/payments/transfer",method="POST"}[20s])/ rate(http_request_duration_seconds_count{handler="/payments/transfer",method="POST"}[20s])',
                    refId='A',
                ),
            ],
            unit=OPS_FORMAT,
            gridPos=GridPos(h=8, w=16, x=0, y=20),
        ),
    ],
).auto_panel_ids()
