from __future__ import division
import glob
import pandas as pd
import matplotlib.pyplot as plt
import numpy as np

line_w=1
marker_s=4
face_c='none'
markers=['s', 'x', '+', '^']
fs=12

def plotGDHM(dirs, oPath):
    subDirs = ["servers/500.1000"] #, "servers/500.100"]
    # frames=[[], [], []]
    frames = []
    files = ["blocksStat_1.csv", "blocksStat_5.csv"]
    for f in files:
        for dir in dirs:
            for d in subDirs:
                path = dir + "/" + d + "/" + f
                df = pd.read_csv(path, sep=",")
                # frames[i].append(df)
                limit = df['propose2decide'].quantile(0.8)
                frames += [df[df.propose2decide <= limit]]
    # df = []
    # for frame in frames:
    #     df += [pd.concat(frame, axis = 0)]
    # df1 = pd.concat(frames, ignore_index=True)
    df = frames
    chan = ['4\n(1)','7\n(1)','10\n(1)',
            '4\n(5)','7\n(5)','10\n(5)']
    labels = ['signature', 'verification', 'propose\ntentative', 'tentative\ndefinite', 'definite\ndecide']
    realLabels=['signaturePeriod','verificationPeriod','propose2tentative','tentative2permanent','channelPermanent2decide','propose2decide']
    normData = []
    # for i in range(0, 3):
    #     data = df[i]
    #     # data = data.mean()
    #     d = data[realLabels[:5]].div(data.propose2decide, axis=0)
    #     normData += [d.mean().values]

    for data in df:
        # data = data.mean()
        d = data[realLabels[:5]].div(data.propose2decide, axis=0)
        normData += [d.mean().values]
    # limit = df1[df1.propose2decide].quantile(0.8)
    # df1 = df1[realLabels[:5]][df1.propose2decide]

    fig, ax = plt.subplots(figsize = (5, 4))
    # im = ax.imshow(normData, cmap='autumn')
    im = ax.imshow(zip(*normData), cmap='Reds')

    # cbar = ax.figure.colorbar(im, ax=ax, cmap="autumn")
    # cbar = plt.colorbar(im, fraction=0.0257, pad=0.04)
    #
    # cbar.ax.set_ylabel("Relative execution time", rotation=-90, va="bottom")
    ax.set_xticks(np.arange(len(chan)))
    ax.set_yticks(np.arange(len(labels)))

    ax.set_xticklabels(chan, fontsize=fs)
    ax.set_yticklabels(labels, fontsize=fs)

    for edge, spine in ax.spines.items():
        spine.set_visible(False)
    ax.set_xticks(np.arange(len(chan) + 1) - .5, minor=True)
    ax.set_yticks(np.arange(len(labels) + 1) - .5, minor=True)
    ax.grid(which="minor", color="black", linestyle='-', linewidth=3)
    ax.tick_params(which="minor", bottom=False, left=False)
    # plt.setp(ax.get_xticklabels(), rotation=45, ha="right",
    #          rotation_mode="anchor")
    # plt.setp(ax.get_yticklabels(), rotation=45, ha="right",
    #          rotation_mode="anchor")

    for i in range(len(chan)):
        for j in range(len(labels)):
            color="black"
            if round(normData[i][j], 3) > 0.4:
                color = 'white'
            text = ax.text(i, j, str(round(normData[i][j], 3)),
                           ha="center", va="center", color=color, fontsize=fs)

    fig.text(0.53, 0.08, "$n(\\omega)$", ha="center", fontsize=fs, va="center")
    fig.tight_layout(rect=[0, 0.09, 1, 1])
    # fig.text(0.07, 0.8, "Channels", ha="center", va="center")
    for d in oPath:
        plt.savefig(d + '/gd_heatmap.pdf', bbox_inches='tight')
        plt.savefig(d + '/gd_heatmap', bbox_inches='tight')

if __name__ == "__main__":
    plotGDHM(["/home/yoni/toy/old/gd_latency/4Servers"
                , "/home/yoni/toy/old/gd_latency/7Servers"
                , "/home/yoni/toy/old/gd_latency/10Servers"]
               , ["/home/yoni/toy/figures", "/home/yoni/Dropbox/paper/draws"])