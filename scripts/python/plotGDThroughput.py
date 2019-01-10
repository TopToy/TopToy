from __future__ import division
import glob
import pandas as pd
import matplotlib.pyplot as plt
import numpy as np

from utiles import csvs2df

fs=12
line_w=1
marker_s=6
face_c='none'
markers=['s', 'x', '+', '^']

def plotGDTrhoughput(dir, oPath):
    rows = 1
    cols = 2
    index = 1
    fig, ax = plt.subplots(nrows=rows, ncols=cols, figsize=(7, 3))
    plt.subplots_adjust(wspace=0.2, hspace=0.5)
    names = ['$\\beta=100$', '$\\beta=1000$']
    subDirs = ['4Servers', '7Servers', '10Servers']
    evSize = ['100', '1000']
    for e in evSize:
        sb = str(rows) + str(cols) + str(index)
        sb = int(sb)
        plt.subplot(sb)
        # summeries = glob.glob(dir + "*/*.100/servers/res/summery.csv")
        m = 0
        for sd in subDirs:
            files = glob.glob(dir + "/" + sd  + "*/*." + e + "/servers/res/summery.csv")
            # path = dir + "/" + sd + "/servers/res/summery.csv"
            mark = markers[m]
            m += 1
            df = csvs2df(files)
            df = df[['channels', 'txPsec']].groupby(df.channels).mean()
            markers_on = [0, 3, 6, 9]
            plt.plot(df['channels'], df['txPsec'] / 1000, "-" + mark,
                     markerfacecolor=face_c, markersize=marker_s,
                     linewidth=line_w, markevery=markers_on)

        plt.title(names[index - 1], fontsize=16)
        plt.xticks(np.arange(0, 21, step=5), fontsize=16)
        plt.yticks(np.arange(0, 15, step=2), fontsize=16)
        plt.grid(True)
        index += 1
    # plt.figlegend(lines, ('label1', 'label2', 'label3'), 'upper right')
    leg = fig.legend([],  # The line objects
               labels=['$n=4$', '$n=7$', '$n=10$'],  # The labels for each line
                     loc="lower center",  # Position of legend
                     # borderaxespad=0.01,  # Small spacing around legend box
                     fontsize=16,
                     ncol=4,
                     frameon=False,
                     bbox_to_anchor=(0.5, -0.04),
               )
    plt.setp(leg.get_title(), fontsize=fs)
    fig.text(0.502, 0.1, "$\\omega$", ha="center", va="center", fontsize=16)
    fig.text(0.02, 0.5, "Throughput ($\\frac{KTx}{sec}$)", ha="center", va="center", rotation=90, fontsize=16)
    fig.tight_layout(rect=[0.02, 0.08, 1, 1])
    for d in oPath:
        plt.savefig(d + '/GD_throughput.pdf', bbox_inches='tight', pad_inches=0.08)
        plt.savefig(d + '/GD_throughput', bbox_inches = 'tight', pad_inches = 0.08)

if __name__ == "__main__":
    plotGDTrhoughput('/home/yoni/toy/gdThroughput', ["/home/yoni/toy/figures", "/home/yoni/Dropbox/paper/draws"])