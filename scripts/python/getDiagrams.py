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


# def loadCsv(path):
#     return pd.read_csv(path)

def drawThroughputDiagram(size, dfs, dfm, dfb, xKey, yKey, outputPath):
#    dfs = pd.DataFrame([[0, 0]], columns=[xKey, yKey]).append(dfs)
#    dfm = pd.DataFrame([[0, 0]], columns=[xKey, yKey]).append(dfm)
#    dfb = pd.DataFrame([[0, 0]], columns=[xKey, yKey]).append(dfb)
    traces = go.Scatter(x = dfs[xKey], y = dfs[yKey]/1000, mode='lines+markers',  marker = dict(size = 7, symbol = 1), name='50B Tx')
    tracem = go.Scatter(x = dfm[xKey], y = dfm[yKey]/1000, mode='lines+markers',  marker = dict(size = 7, symbol = 2), name='1KB Tx')
    traceb = go.Scatter(x = dfb[xKey], y = dfb[yKey]/1000, mode='lines+markers',  marker = dict(size = 7, symbol = 4), name='4KB Tx')
    ys = range(0,500, 30)
    # layout = go.Layout(title=' Throughput (over different channels)',
    #                plot_bgcolor='rgb(230, 230, 230)')
    layout = dict(title='Throughput',
                  xaxis=dict(
                      title='Channels',
                      tickformat=',d'),
                  yaxis=dict(title='KTxs/Sec')
#                  tickvals=ys,  showticklabels=True)
                  )
    fig = go.Figure(data=[traces, tracem, traceb], layout=layout)
    # plotly.offline.plot(fig, filename='Toy_Throughput')
    if not os.path.exists(outputPath + '/images'):
        os.mkdir(outputPath + '/images')
    pio.write_image(fig, outputPath + '/images/fig.' + size + '.jpeg')


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Create a Toy Diagrams')
    parser.add_argument('-s', metavar='path', required=True,
                        help='the path to the directory of the 50 byte transactions ')
    parser.add_argument('-m', metavar='path', required=True,
                        help='the path to the directory of the 1K byte transactions ')
    parser.add_argument('-b', metavar='path', required=True,
                        help='the path to the directory of the 4K byte transactions ')
    parser.add_argument('-o', metavar='path', required=True,
                        help='the path to the output directory ')
    args = parser.parse_args()
    dfs = pd.read_csv(args.s + '.10/res/summery.csv', sep=",")
    dfm = pd.read_csv(args.m + '.10/res/summery.csv', sep=",")
    dfb = pd.read_csv(args.b + '.10/res/summery.csv', sep=",")
    drawThroughputDiagram('10', dfs[['channels', 'txPsec']].groupby(dfs.channels).mean(), dfm[['channels', 'txPsec']].groupby(dfm.channels).mean(), dfb[['channels', 'txPsec']].groupby(dfb.channels).mean(), 'channels', 'txPsec', args.o)
    dfs = pd.read_csv(args.s + '.100/res/summery.csv', sep=",")
    dfm = pd.read_csv(args.m + '.100/res/summery.csv', sep=",")
    dfb = pd.read_csv(args.b + '.100/res/summery.csv', sep=",")
    drawThroughputDiagram('100', dfs[['channels', 'txPsec']].groupby(dfs.channels).mean(), dfm[['channels', 'txPsec']].groupby(dfm.channels).mean(), dfb[['channels', 'txPsec']].groupby(dfb.channels).mean(), 'channels', 'txPsec', args.o)
    dfs = pd.read_csv(args.s + '.1000/res/summery.csv', sep=",")
    dfm = pd.read_csv(args.m + '.1000/res/summery.csv', sep=",")
    dfb = pd.read_csv(args.b + '.1000/res/summery.csv', sep=",")
    drawThroughputDiagram('1000', dfs[['channels', 'txPsec']].groupby(dfs.channels).mean(), dfm[['channels', 'txPsec']].groupby(dfm.channels).mean(), dfb[['channels', 'txPsec']].groupby(dfb.channels).mean(), 'channels', 'txPsec', args.o)