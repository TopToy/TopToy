from __future__ import division
import os
import plotly
import plotly.plotly as py
import plotly.graph_objs as go
import plotly.figure_factory as FF
import argparse
import plotly.io as pio

import numpy as np
import pandas as pd
from plotly import tools


def drawSigCharts(dirs, oPath):
    fig = tools.make_subplots(rows=1, cols=2, subplot_titles=('50B Tx 10 Txs/block', '4KB Tx 1000 Txs/block'))
    df = pd.read_csv(dirs[0], sep=",")
    df = df[['channels', 'sigAvg']]
    trace = go.Scatter(x=df['channels'], y=df['sigAvg'] / 1000, name='50B')
    fig.append_trace(trace, 1, 1)
    df = pd.read_csv(dirs[1], sep=",")
    df = df[['channels', 'sigAvg']]
    trace = go.Scatter(x=df['channels'], y=df['sigAvg'] / 1000, name='4096B')
    fig.append_trace(trace, 1, 2)
    for i in fig['layout']['annotations']:
        i['font'] = dict(size=12)
    for i in range(1, 3):
        fig['layout']['xaxis' + str(i)].update(rangemode='tozero', tickformat=',d', title='channels', titlefont=dict(size=12))
        fig['layout']['yaxis' + str(i)].update(rangemode='tozero', tickformat=',d')
        fig['layout']['yaxis' + str(i)].update(title='Ksignatures/sec', titlefont=dict(size=12))
   # fig['layout'].update(height=800, width=800)
    pio.write_image(fig, oPath + 'res2.jpeg')

def drawCharts(dirs, oPath):
    fig = tools.make_subplots(rows=3, cols=3, subplot_titles=('4 servers 10 Txs/block','4 servers 100 Txs/block'
                                                              , '4 servers 1000 Txs/block', '7 servers 10 Txs/block'
                                                              , '7 servers 100 Txs/block', '7 servers 1000 Txs/block'
                                                              ,'10 servers 10 Txs/block', '10 servers 100 Txs/block'
                                                              , '10 servers 1000 Txs/block'))
    plots=[(1, 1), (1, 2), (1, 3), (2, 1), (2, 2), (2, 3), (3, 1), (3, 2), (3, 3)]
    evSize=['10', '100', '1000']
    bSize=['50', '512', '1024', '4096']
    markers=[1, 2, 3, 4]
    colors=['blue', 'orange', 'red', 'green']
    iter = 0
    for i in range(0, 3):
        dir = dirs[i]
        for j in range(0, 3):
            p = plots[iter]
            es = evSize[j]
            iter += 1
            for k in range(0, 4):
                s =bSize[k]
                c = colors[k]
                m = markers[k]
                df = pd.read_csv(dir + '/' + es + '/' + s + '.csv', sep=",")
                df = df[['channels', 'txPsec']].groupby(df.channels).mean()
                if (i == 0 and j == 0):
                    trace = go.Scatter(x = df['channels'], y = df['txPsec']/1000, name=s + 'B',
                                       legendgroup=s, line= {'color': c}, mode='lines')
                                       #, marker = dict(size = 5, symbol = m))
                else:
                    trace = go.Scatter(x=df['channels'], y=df['txPsec'] / 1000, name=s + 'B',
                                       showlegend=False, legendgroup=s, line= {'color': c}, mode='lines')
                                      # , marker = dict(size = 5, symbol = m))
                fig.append_trace(trace, p[0], p[1])
    for i in fig['layout']['annotations']:
        i['font'] = dict(size=12)
    for i in range(1, 10):
        fig['layout']['xaxis' + str(i)].update(rangemode='tozero', tickformat=',d')
        fig['layout']['yaxis' + str(i)].update(rangemode='tozero', tickformat=',d')
        if (i == 8):
            fig['layout']['xaxis' + str(i)].update(title='channels', titlefont=dict(size=16))
        if (i == 4):
            fig['layout']['yaxis' + str(i)].update(title='KTxs/sec', titlefont=dict(size=16))
    fig['layout'].update(height=800, width=800)
    pio.write_image(fig, oPath + 'res1.jpeg')


# def loadCsv(path):
#     return pd.read_csv(path)

def drawThroughputDiagram(size, dfe, dfs, dfm, dfb, xKey, yKey, outputPath):
#    dfs = pd.DataFrame([[0, 0]], columns=[xKey, yKey]).append(dfs)
#    dfm = pd.DataFrame([[0, 0]], columns=[xKey, yKey]).append(dfm)
#    dfb = pd.DataFrame([[0, 0]], columns=[xKey, yKey]).append(dfb)
    tracee = go.Scatter(x = dfe[xKey], y = dfe[yKey]/1000, mode='lines+markers',  marker = dict(size = 7, symbol = 5), name='50B Tx')
    traces = go.Scatter(x = dfs[xKey], y = dfs[yKey]/1000, mode='lines+markers',  marker = dict(size = 7, symbol = 1), name='512B Tx')
    tracem = go.Scatter(x = dfm[xKey], y = dfm[yKey]/1000, mode='lines+markers',  marker = dict(size = 7, symbol = 2), name='1KB Tx')
    traceb = go.Scatter(x = dfb[xKey], y = dfb[yKey]/1000, mode='lines+markers',  marker = dict(size = 7, symbol = 4), name='4KB Tx')
    ys = range(0,500, 30)
    # layout = go.Layout(title=' Throughput (over different channels)',
    #                plot_bgcolor='rgb(230, 230, 230)')
    layout = dict(
                  xaxis=dict(rangemode='tozero'
                      , title='Channels'
                      , tickformat=',d'),
                  yaxis=dict(title='KTxs/Sec'
                  ,rangemode='tozero')
#                  tickvals=ys,  showticklabels=True)
                    )
    fig = go.Figure(data=[tracee, traces, tracem, traceb], layout=layout)
    # plotly.offline.plot(fig, filename='Toy_Throughput')
   # if not os.path.exists(outputPath + '/images'):
    #    os.mkdir(outputPath + '/images')
    pio.write_image(fig, outputPath + size + '.jpeg')


if __name__ == '__main__':
    base = '/home/yoni/toy/plots'
    # dirs=[base + '/4servers', base + '/7servers', base + '/10servers']
    # drawCharts(dirs, '/home/yoni/Dropbox/paper/draws/')
    dirs = [base + '/sig/0-10.csv', base + '/sig/1000-4096.csv']
    drawSigCharts(dirs, '/home/yoni/Dropbox/paper/draws/')
#     parser = argparse.ArgumentParser(description='Create a Toy Diagrams')
#     parser.add_argument('-t', metavar='path', required=True,
#                             help='Tx in block ')
#     parser.add_argument('-e', metavar='path', required=True,
#                         help='the path to the directory of the 50 byte transactions ')
#     parser.add_argument('-s', metavar='path', required=True,
#                         help='the path to the directory of the 512 byte transactions ')
#     parser.add_argument('-m', metavar='path', required=True,
#                             help='the path to the directory of the 1K byte transactions ')
#     parser.add_argument('-b', metavar='path', required=True,
#                         help='the path to the directory of the 4K byte transactions ')
#     parser.add_argument('-o', metavar='path', required=True,
#                         help='the path to the output directory ')
#     args = parser.parse_args()
# #    dfs = pd.read_csv(args.s + '.10/res/summery.csv', sep=",")
# #    dfm = pd.read_csv(args.m + '.10/res/summery.csv', sep=",")
# #    dfb = pd.read_csv(args.b + '.10/res/summery.csv', sep=",")
# #    drawThroughputDiagram('10', dfs[['channels', 'txPsec']].groupby(dfs.channels).mean(), dfm[['channels', 'txPsec']].groupby(dfm.channels).mean(), dfb[['channels', 'txPsec']].groupby(dfb.channels).mean(), 'channels', 'txPsec', args.o)
# #    dfs = pd.read_csv(args.s + '.100/res/summery.csv', sep=",")
# #    dfm = pd.read_csv(args.m + '.100/res/summery.csv', sep=",")
# #    dfb = pd.read_csv(args.b + '.100/res/summery.csv', sep=",")
# #    drawThroughputDiagram('100', dfs[['channels', 'txPsec']].groupby(dfs.channels).mean(), dfm[['channels', 'txPsec']].groupby(dfm.channels).mean(), dfb[['channels', 'txPsec']].groupby(dfb.channels).mean(), 'channels', 'txPsec', args.o)
#     dfe = pd.read_csv(args.e + '/res/summery.csv', sep=",")
#     dfs = pd.read_csv(args.s + '/res/summery.csv', sep=",")
#     dfm = pd.read_csv(args.m + '/res/summery.csv', sep=",")
#     dfb = pd.read_csv(args.b + '/res/summery.csv', sep=",")
#     drawThroughputDiagram(args.t, dfe[['channels', 'txPsec']].groupby(dfe.channels).mean(), dfs[['channels', 'txPsec']].groupby(dfs.channels).mean(), dfm[['channels', 'txPsec']].groupby(dfm.channels).mean(), dfb[['channels', 'txPsec']].groupby(dfb.channels).mean(), 'channels', 'txPsec', args.o)