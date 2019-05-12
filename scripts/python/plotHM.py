from __future__ import division
import glob
import pandas as pd
import matplotlib.pyplot as plt
import numpy as np

from utiles import csvs2df

fs=14

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
    chan = ['4\n(1)', '7\n(1)', '10\n(1)',
            '4\n(5)', '7\n(5)', '10\n(5)',
            '4\n(10)', '7\n(10)', '10\n(10)']
    labels = ['signature', 'verification', 'propose-\ntentative', 'tentative-\ndefinite', 'definite-\ndecide']
    realLabels = ['signaturePeriod', 'verificationPeriod', 'propose2tentative', 'tentative2permanent',
                  'channelPermanent2decide', 'propose2decide']
    normData = []
    for data in df:
        d = data[realLabels[:5]].div(data.propose2decide, axis=0)
        normData += [d.mean().values]
    fig, ax = plt.subplots()
    im = ax.imshow(zip(*normData), cmap='Reds')

    # cbaxes = fig.add_axes([0, 0.1, 0.03, 0.8])
    # cbar = plt.colorbar(im, cax=cbaxes, fraction=0.0258, pad=0.04)


    ax.set_xticks(np.arange(len(chan)))
    ax.set_yticks(np.arange(len(labels)))

    ax.set_xticklabels(chan, fontsize=fs)
    ax.set_yticklabels(labels, fontsize=fs)

    ax.set_xticks(np.arange(len(chan) + 1) - .5, minor=True)
    ax.set_yticks(np.arange(len(labels) + 1) - .5, minor=True)

    # cbar.ax.set_location("bottom")
    # cbar.ax.set_ylabel("Relative execution time", rotation=0, va="bottom", fontsize=fs)
    # cbar = plt.colorbar(im, fraction=0.28, pad=0.15, orientation='horizontal')
    # fig.text(0.53, 0, "Relative execution time", ha="center", fontsize=fs, va="center")
    for edge, spine in ax.spines.items():
        spine.set_visible(False)

    ax.grid(which="minor", color="black", linestyle='-', linewidth=3)
    ax.tick_params(which="minor", bottom=False, left=False)
    plt.setp(ax.get_xticklabels(),
             rotation_mode="anchor", fontsize=fs)
    for i in range(len(chan)):
        for j in range(len(labels)):
            clr='black'
            if round(normData[i][j], 2) >= 0.45:
                clr='white'
            text = ax.text(i, j, str(round(normData[i][j], 2)),
                           ha="center", va="center", color=clr, fontsize=fs)
    fig.text(0.53, 0.08, "$n(\\omega)$", ha="center", fontsize=fs, va="center")
    fig.tight_layout(rect=[0, 0.01, 1, 1])
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
    chan = ['$\\nu=4$,\n$\\omega=1$', '$\\nu=7$\n$\\omega=1$', '$\\nu=10$\n$\\omega=1$',
            '$\\nu=4$,\n$\\omega=5$', '$\\nu=7$,\n$\\omega=5$', '$\\nu=10$,\n$\\omega=5$',
            '$\\nu=4$,\n$\\omega=5$', '$\\nu=7$,\n$\\omega=5$', '$\\nu=10$,\n$\\omega=5$']
    labels = ['signature', 'verification', 'propose-\ntentative', 'tentative-\ndefinite', 'definite-\ndecide']
    realLabels = ['signaturePeriod', 'verificationPeriod', 'propose2tentative', 'tentative2permanent',
                  'channelPermanent2decide', 'propose2decide']
    normData = []
    for data in df:
        d = data[realLabels[:5]].div(data.propose2decide, axis=0)
        normData += [d.mean().values]
    fig, ax = plt.subplots()
    im = ax.imshow(normData, cmap='autumn')
    # cbar = plt.colorbar(im, fraction=0.037, pad=0.01)

    ax.set_xticks(np.arange(len(labels)))
    ax.set_yticks(np.arange(len(chan)))

    ax.set_xticklabels(labels, fontsize=fs)
    ax.set_yticklabels(chan, fontsize=fs)

    ax.set_xticks(np.arange(len(labels) + 1) - .5, minor=True)
    ax.set_yticks(np.arange(len(chan) + 1) - .5, minor=True)

    # cbar.ax.set_ylabel("Relative execution time", rotation=-90, va="bottom")

    for edge, spine in ax.spines.items():
        spine.set_visible(False)

    ax.grid(which="minor", color="black", linestyle='-', linewidth=3)
    ax.tick_params(which="minor", bottom=False, left=False)
    plt.setp(ax.get_xticklabels(), rotation=45, ha="right",
             rotation_mode="anchor")
    for i in range(len(labels)):
        for j in range(len(chan)):
            text = ax.text(i, j, str(round(normData[j][i], 2)),
                           ha="center", va="center", color="black", fontsize=fs)
    fig.tight_layout(rect=[0, 0, 1.05, 1])
    for d in oPath:
        plt.savefig(d + '/heatmap_t.pdf', bbox_inches='tight')
        plt.savefig(d + '/heatmap_t', bbox_inches='tight')


def plotHM(dirs, oPath, transpos):
    if transpos == False:
        plotNormal(dirs, oPath)
    if transpos == True:
        plotT(dirs, oPath)
if __name__ == "__main__":
    plotHM(["/home/yoni/toy/oldold/old/latency/4Servers"
                 , "/home/yoni/toy/oldold/old/latency/7Servers"
                 , "/home/yoni/toy/oldold/old/latency/10Servers"]
             , ["/home/yoni/toy/figures", "/home/yoni/Dropbox/paper/draws"], False)