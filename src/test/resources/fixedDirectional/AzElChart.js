const data = {
    labels: ['0', '45', '90', '135', '180', '225', '270', '315'],
    datasets: dses
};

const options = {
        elements: {
            point: {
                  radius: 0,
                  hitRadius: 10,
                  hoverRadius: 4,
                  hoverBorderWidth: 3
            }
        },
        animation: {
            duration: 0
        },
        scale: {
            type: 'myScale',
            angleLines: {
                display: true
            },
            gridLines: {
                circular: true
            },
                        ticks: {
                display: false,
                beginAtZero: true,
                max: 90,
                min: 0
            }
        },
        legend: {
            display: false
        },
        tooltips: {
            displayColors: false,
            callbacks: {
                title: function(tooltipItem, data) {
                    return data.datasets[tooltipItem[0].datasetIndex].data[tooltipItem[0].index].time;
                    //console.log(tooltipItem)
                    //return '';
                },
                label: function(tooltipItem, data) {
                    let item = data.datasets[tooltipItem.datasetIndex].data[tooltipItem.index];
                    return [
                        "AZ: " + item.x,
                        "EL: " + item.y
                    ];
                }
            }
        }
};

Chart.defaults.derivedRadar= Chart.defaults.radar;

 var custom = Chart.controllers.radar.extend({
    update: function(reset) {
        var me = this;
        var meta = me.getMeta();
        var line = meta.dataset;
        var points = meta.data || [];
        var scale = me.chart.scale;
        var config = me._config;
        var i, ilen;

        // Compatibility: If the properties are defined with only the old name, use those values
        if (config.tension !== undefined && config.lineTension === undefined) {
            config.lineTension = config.tension;
        }

        // Utility
        line._scale = scale;
        line._datasetIndex = me.index;
        // Data
        line._children = points;
        line._loop = false;
        // Model
        line._model = me._resolveDatasetElementOptions(line);

        line.pivot();

        // Update Points
        for (i = 0, ilen = points.length; i < ilen; ++i) {
            me.updateElement(points[i], i, reset);
        }

        // Update bezier control points
        me.updateBezierControlPoints();

        // Now pivot the point for animation
        for (i = 0, ilen = points.length; i < ilen; ++i) {
            points[i].pivot();
        }
    }
});
Chart.controllers.derivedRadar = custom;

let MyScale = Chart.scaleService.getScaleConstructor('radialLinear').extend({
    getPointPositionForValue: function(index, value) {
        var me = this;
        var scalingFactor = me.drawingArea / (me.max - me.min);
        return {
            y:  scalingFactor * Math.cos(value.x * Math.PI / 180.0) * (value.y - 90) + me.yCenter,
            x:  scalingFactor * Math.sin(value.x * Math.PI / 180.0) * (90 - value.y) + me.xCenter
        };
    },
    getDistanceFromCenterForValue: function(value) {
        var me = this;
        // Take into account half font size + the yPadding of the top value
        var scalingFactor = me.drawingArea / (me.max - me.min);
        if (me.options.ticks.reverse) {
            return (me.max - value) * scalingFactor;
        }
        return (value - me.min) * scalingFactor;
    }
});
Chart.scaleService.registerScaleType('myScale', MyScale, {
    display: true,

    // Boolean - Whether to animate scaling the chart from the centre
    animate: false,
    position: 'chartArea',

    angleLines: {
        display: true,
        color: 'rgba(0,0,0,0.1)',
        lineWidth: 1,
        borderDash: [],
        borderDashOffset: 0.0
    },

    gridLines: {
        circular: false
    },

    // label settings
    ticks: {
        // Boolean - Show a backdrop to the scale label
        showLabelBackdrop: true,

        // String - The colour of the label backdrop
        backdropColor: 'rgba(255,255,255,0.75)',

        // Number - The backdrop padding above & below the label in pixels
        backdropPaddingY: 2,

        // Number - The backdrop padding to the side of the label in pixels
        backdropPaddingX: 2
        
    },

    pointLabels: {
        // Boolean - if true, show point labels
        display: true,

        // Number - Point label font size in pixels
        fontSize: 10,

        // Function - Used to convert point labels
        callback: function(label) {
            return label;
        }
    }
});

// Get the canvas element
const ctx = document.getElementById('radarChart').getContext('2d');

// Create the radar chart
const radarChart = new Chart(ctx, {
    type: 'derivedRadar',
    data: data,
    options: options
});
