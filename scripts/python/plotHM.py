from __future__ import division
import glob
import pandas as pd
import matplotlib.pyplot as plt
import numpy as np

from utiles import csvs2df

line_w=1
marker_s=4
face_c='none'
markers=['s', 'x', '+', '^']

def plotNormal(dirs, oPath):
    subDirs = ["servers/500.1000"]  # , "servers/500.100"]
    # frames=[[], [], []]
    frames = []
    files = ["blocksStat_1.csv", "blocksStat_5.csv", "blocksStat_10.csv"]
    for f in files:
        for dir in dirs:
            for d in subDirs:
                path = dir + "/" + d + "/" + f
                df = pd.read_csv(path, sep=",")
                # frames[i].append(df)
                frames += [df]
    # df = []
    # for frame in frames:
    #     df += [pd.concat(frame, axis = 0)]

    df = frames
    chan = ['4 servers\n1 channel', '7 servers\n1 channel', '10 servers\n1 channel',
            '4 servers\n5 channels', '7 servers\n5 channels', '10 servers\n5 channels',
            '4 servers\n10 channels', '7 servers\n10 channels', '10 servers\n10 channels']
    labels = ['signature', 'verification', 'propose\ntentative', 'tentative\npermanent', 'permanent\ndecide']
    realLabels = ['signaturePeriod', 'verificationPeriod', 'propose2tentative', 'tentative2permanent',
                  'channelPermanent2decide', 'propose2decide']
    normData = []
    for data in df:
        d = data[realLabels[:5]].div(data.propose2decide, axis=0)
        normData += [d.mean().values]
    fig, ax = plt.subplots()
    im = ax.imshow(zip(*normData), cmap='autumn')
    cbar = plt.colorbar(im, fraction=0.0258, pad=0.04)

    ax.set_xticks(np.arange(len(chan)))
    ax.set_yticks(np.arange(len(labels)))

    ax.set_xticklabels(chan, fontsize='x-small')
    ax.set_yticklabels(labels, fontsize='small')

    ax.set_xticks(np.arange(len(chan) + 1) - .5, minor=True)
    ax.set_yticks(np.arange(len(labels) + 1) - .5, minor=True)

    cbar.ax.set_ylabel("Relative execution time", rotation=-90, va="bottom")

    for edge, spine in ax.spines.items():
        spine.set_visible(False)

    ax.grid(which="minor", color="black", linestyle='-', linewidth=3)
    ax.tick_params(which="minor", bottom=False, left=False)
    plt.setp(ax.get_xticklabels(), rotation=45, ha="right",
             rotation_mode="anchor")
    for i in range(len(chan)):
        for j in range(len(labels)):
            text = ax.text(i, j, str(round(normData[i][j], 3)),
                           ha="center", va="center", color="black")
    for d in oPath:
        plt.savefig(d + '/heatmap_n.pdf', bbox_inches='tight')
        plt.savefig(d + '/heatmap_n', bbox_inches='tight')


def plotT(dirs, oPath):
    subDirs = ["servers/500.1000"]  # , "servers/500.100"]
    # frames=[[], [], []]
    frames = []
    files = ["blocksStat_1.csv", "blocksStat_5.csv", "blocksStat_10.csv"]
    for f in files:
        for dir in dirs:
            for d in subDirs:
                path = dir + "/" + d + "/" + f
                df = pd.read_csv(path, sep=",")
                # frames[i].append(df)
                frames += [df]
    # df = []
    # for frame in frames:
    #     df += [pd.concat(frame, axis = 0)]

    df = frames
    chan = ['4 servers\n1 channel', '7 servers\n1 channel', '10 servers\n1 channel',
            '4 servers\n5 channels', '7 servers\n5 channels', '10 servers\n5 channels',
            '4 servers\n10 channels', '7 servers\n10 channels', '10 servers\n10 channels']
    labels = ['signature', 'verification', 'propose\ntentative', 'tentative\ndefinite', 'definite\ndecide']
    realLabels = ['signaturePeriod', 'verificationPeriod', 'propose2tentative', 'tentative2permanent',
                  'channelPermanent2decide', 'propose2decide']
    normData = []
    for data in df:
        d = data[realLabels[:5]].div(data.propose2decide, axis=0)
        normData += [d.mean().values]
    fig, ax = plt.subplots()
    im = ax.imshow(normData, cmap='autumn')
    cbar = plt.colorbar(im, fraction=0.037, pad=0.04)

    ax.set_xticks(np.arange(len(labels)))
    ax.set_yticks(np.arange(len(chan)))

    ax.set_xticklabels(labels, fontsize='small')
    ax.set_yticklabels(chan, fontsize='small')

    ax.set_xticks(np.arange(len(labels) + 1) - .5, minor=True)
    ax.set_yticks(np.arange(len(chan) + 1) - .5, minor=True)

    cbar.ax.set_ylabel("Relative execution time", rotation=-90, va="bottom")

    for edge, spine in ax.spines.items():
        spine.set_visible(False)

    ax.grid(which="minor", color="black", linestyle='-', linewidth=3)
    ax.tick_params(which="minor", bottom=False, left=False)
    plt.setp(ax.get_xticklabels(), rotation=45, ha="right",
             rotation_mode="anchor")
    for i in range(len(labels)):
        for j in range(len(chan)):
            text = ax.text(i, j, str(round(normData[j][i], 3)),
                           ha="center", fontsize='x-small', va="center", color="black")
    for d in oPath:
        plt.savefig(d + '/heatmap_t.pdf', bbox_inches='tight')
        plt.savefig(d + '/heatmap_t', bbox_inches='tight')


def plotHM(dirs, oPath, transpos):
    if transpos == False:
        plotNormal(dirs, oPath)
    if transpos == True:
        plotT(dirs, oPath)
if __name__ == "__main__":
    plotHM(["/home/yoni/toy/old/latency/4Servers"
                 , "/home/yoni/toy/old/latency/7Servers"
                 , "/home/yoni/toy/old/latency/10Servers"]
             , ["/home/yoni/toy/figures", "/home/yoni/Dropbox/paper/draws"], False)