
from __future__ import division
import glob
import pandas as pd
import matplotlib.pyplot as plt
import numpy as np

from utiles import csvs2df
fs=14

line_w=1.7
marker_s=7
face_c='none'
markers=['s', 'x', '+', '^']

def getYrange(index):
    if index == 1:
        return np.arange(0, 1201, 200)
    if index == 2:
        return np.arange(0, 26, 5)
    if index == 3:
        return np.arange(0, 161, 40)
    if index == 4:
        return np.arange(0, 17, 4)
    if index == 5:
        return np.arange(0, 81, 20)
    if index == 6:
        return np.arange(0, 161, 40)
    if index == 7:
        return np.arange(0, 17, 4)
    if index == 8:
        return np.arange(0, 81, 20)
    if index == 9:
        return np.arange(0, 161, 40)

def tps_cs(dirs, oPath):
    rows = 1
    cols = 2
    index = 1

    n = 0
    txSize = [0, 128, 1024]
    fig, ax = plt.subplots(nrows=rows, ncols=cols, figsize=(8.5, 4))
    plt.subplots_adjust(wspace=2, hspace=0)
    lines = []
    sb = str(rows) + str(cols) + str(index)
    sb = int(sb)
    plt.subplot(sb)
    for d in dirs:
        files = glob.glob(d + "/**/summeries/sum.csv")
        df = csvs2df(files)
        m = 0
        for tx in txSize:
            data = df[(df.txSize == tx)]
            data = data[['cs', 'tps']]
            data = data[['cs', 'tps']].groupby(df.cs).mean()
            mark = markers[m]
            m += 1
            plt.plot(data['cs'], data['tps'] / 1000, "-" + mark, markerfacecolor=face_c,
                     markersize=marker_s, linewidth=line_w)  # , markevery=markers_on
    plt.plot(np.array([4, 8, 16, 32, 64, 128]), np.array([190, 160, 150, 110, 90, 50]), "-v", markerfacecolor=face_c,
             markersize=marker_s, linewidth=line_w)
    plt.plot(np.array([4, 8, 16, 32, 64, 128]), np.array([150, 145, 140, 100, 80, 50]), "-o", markerfacecolor=face_c,
             markersize=marker_s, linewidth=line_w)
    plt.plot(np.array([4, 8, 16, 32, 64, 128]), np.array([60, 55, 50, 40, 35, 25]), "->", markerfacecolor=face_c,
             markersize=marker_s, linewidth=line_w)
    plt.title("FLO Vs bft-SMaRt\nThroughput", fontsize=fs-1)
    plt.xticks(np.array([4, 8, 16, 32, 64, 128]), fontsize=fs-1)
    lbs = ['0', '2$\cdot$10$^5$', '4$\cdot$10$^5$', '6$\cdot$10$^5$', '8$\cdot$10$^5$', '10$^6$', '1.2$\cdot$10$^6$']
    plt.yticks(getYrange(index), lbs, fontsize=fs - 1)
    plt.grid(True)
    n += 1
    index += 1


    sb = str(rows) + str(cols) + str(index)
    sb = int(sb)
    plt.subplot(sb)
    for d in dirs:
        files = glob.glob(d + "/**/summeries/sum.csv")
        df = csvs2df(files)
        m = 0
        for tx in txSize:
            data = df[(df.txSize == tx)]
            data = data[['cs', 'BP2DL']]
            data = data[['cs', 'BP2DL']].groupby(df.cs).mean()
            mark = markers[m]
            m += 1
            plt.plot(data['cs'], data['BP2DL'] / 1000, "-" + mark, markerfacecolor=face_c,
                     markersize=marker_s, linewidth=line_w)  # , markevery=markers_on
    plt.plot(np.array([4, 8, 16, 32, 64, 128]), np.array([0.003, 0.004, 0.005, 0.01, 0.015, 0.023]), "-v",
             markerfacecolor=face_c,
             markersize=marker_s, linewidth=line_w)
    plt.plot(np.array([4, 8, 16, 32, 64, 128]), np.array([0.01, 0.01, 0.02, 0.03, 0.04, 0.05]), "-o",
             markerfacecolor=face_c,
             markersize=marker_s, linewidth=line_w)
    plt.plot(np.array([4, 8, 16, 32, 64, 128]), np.array([0.02, 0.03, 0.04, 0.05, 0.07, 0.12]), "->",
             markerfacecolor=face_c,
             markersize=marker_s, linewidth=line_w)
    plt.title("FLO Vs bft-SMaRt\nLatency", fontsize=fs)
    plt.xticks(np.array([4, 8, 16, 32, 64, 128]), fontsize=fs-1)
    plt.yticks(getYrange(index), fontsize=fs-1)
    plt.grid(True)
    n += 1
    index += 1


    leg = fig.legend(lines,  # The line objects
                     labels=['FL-0B', 'FL-128B', 'FL-1KB', 'bft-SMaRt-0B', 'bft-SMaRt-128B', 'bft-SMaRt-1KB'],
                     # The labels for each line
                     loc="lower center",  # Position of legend
                     # borderaxespad=0.01,  # Small spacing around legend box
                     fontsize=12,
                     ncol=2,
                     frameon=False,
                     bbox_to_anchor=(0.5, -0.03),
                     #  title = "Tx size\n(Bytes)"
                     )
    # plt.setp(leg.get_title(), fontsize='xx-small')
    fig.text(0.5, 0.21, "$n$", ha="center", va="center", fontsize=fs)
    fig.text(0.03, 0.55, "TPS ($\\frac{Transactions}{sec}$)", ha="center", va="center", fontsize=fs, rotation=90)
    fig.text(0.543, 0.55, "Latency (sec)", ha="center", va="center", fontsize=fs, rotation=90)
    fig.tight_layout(rect=[0.02, 0.2, 1.02, 1.0])
    for d in oPath:
        plt.savefig(d + '/tps_cs_bfts.pdf')
        plt.savefig(d + '/tps_cs_bfts')

if __name__ == "__main__":
    tps_cs(["/home/yon/toy/c5"],
        ["/home/yon/toy/c5/figures", "/home/yon/Dropbox/paper/figures"])