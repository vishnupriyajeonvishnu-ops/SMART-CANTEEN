/**
 * DMI College of Engineering - Smart Canteen Management System
 * Chart.js visualization helpers for Management Analytics
 */

export class AnalyticsCharts {
    /**
     * Render 7-day or 30-day Revenue Trend Area Chart
     */
    static renderRevenueTrend(canvasId, labels, values) {
        const ctx = document.getElementById(canvasId);
        if (!ctx || !window.Chart) return null;

        return new window.Chart(ctx, {
            type: 'line',
            data: {
                labels: labels,
                datasets: [{
                    label: 'Revenue (₹)',
                    data: values,
                    borderColor: '#f59e0b',
                    backgroundColor: 'rgba(245, 158, 11, 0.15)',
                    borderWidth: 2.5,
                    fill: true,
                    tension: 0.35,
                    pointBackgroundColor: '#d97706',
                    pointRadius: 4,
                    pointHoverRadius: 6
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: { display: false },
                    tooltip: {
                        callbacks: {
                            label: (context) => `Revenue: ₹${Number(context.raw).toLocaleString('en-IN')}`
                        }
                    }
                },
                scales: {
                    y: {
                        beginAtZero: true,
                        ticks: {
                            callback: (value) => '₹' + value
                        },
                        grid: { color: 'rgba(148, 163, 184, 0.15)' }
                    },
                    x: {
                        grid: { display: false }
                    }
                }
            }
        });
    }

    /**
     * Render Online (UPI) vs Cash Donut Chart
     */
    static renderPaymentMethodDonut(canvasId, onlineAmount, cashAmount) {
        const ctx = document.getElementById(canvasId);
        if (!ctx || !window.Chart) return null;

        const total = (Number(onlineAmount) || 0) + (Number(cashAmount) || 0);

        return new window.Chart(ctx, {
            type: 'doughnut',
            data: {
                labels: ['GPay / UPI', 'Cash Counter'],
                datasets: [{
                    data: [Number(onlineAmount) || 0, Number(cashAmount) || 0],
                    backgroundColor: ['#f59e0b', '#0f382a'],
                    borderWidth: 0,
                    hoverOffset: 6
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: {
                        position: 'bottom',
                        labels: { boxWidth: 12, padding: 15 }
                    },
                    tooltip: {
                        callbacks: {
                            label: (context) => {
                                const val = Number(context.raw);
                                const pct = total > 0 ? ((val / total) * 100).toFixed(1) : '0';
                                return ` ${context.label}: ₹${val.toLocaleString('en-IN')} (${pct}%)`;
                            }
                        }
                    }
                },
                cutout: '72%'
            }
        });
    }

    /**
     * Render Top Selling Foods Horizontal Bar Chart
     */
    static renderTopFoodsBar(canvasId, items = []) {
        const ctx = document.getElementById(canvasId);
        if (!ctx || !window.Chart) return null;

        const labels = items.map(i => i.itemName);
        const quantities = items.map(i => i.totalQuantity);

        return new window.Chart(ctx, {
            type: 'bar',
            data: {
                labels: labels,
                datasets: [{
                    label: 'Units Sold',
                    data: quantities,
                    backgroundColor: 'rgba(245, 158, 11, 0.85)',
                    borderRadius: 6,
                    borderSkipped: false
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                indexAxis: 'y',
                plugins: {
                    legend: { display: false },
                    tooltip: {
                        callbacks: {
                            label: (context) => ` Sold: ${context.raw} orders`
                        }
                    }
                },
                scales: {
                    x: {
                        beginAtZero: true,
                        ticks: { precision: 0 },
                        grid: { color: 'rgba(148, 163, 184, 0.15)' }
                    },
                    y: {
                        grid: { display: false }
                    }
                }
            }
        });
    }
}
