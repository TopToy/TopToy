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

def plotGDTrhoughput(dir, oPath):
    rows = 1
    cols = 2
    index = 1
    fig, ax = plt.subplots(nrows=rows, ncols=cols)
    plt.subplots_adjust(wspace=0.2, hspace=0.5)
    names = ['100 Txs/block', '1000 Txs/block']
    subDirs = ['4Servers', '7Servers', '10Servers']
    evSize = ['100', '1000']
    for e in evSize:
        sb = str(rows) + str(cols) + str(index)
        sb = int(sb)
        plt.subplot(sb, aspect='equal', adjustable='box-forced')
        # summeries = glob.glob(dir + "*/*.100/servers/res/summery.csv")
        m = 0
        for sd in subDirs:
            files = glob.glob(dir + "/" + sd  + "*/*." + e + "/servers/res/summery.csv")
            # path = dir + "/" + sd + "/servers/res/summery.csv"
            mark = markers[m]
            m += 1
            df = csvs2df(files)
            df = df[['channels', 'txPsec']].groupby(df.channels).mean()
            markers_on = [0, 5, 10, 19]
            plt.plot(df['channels'], df['txPsec'] / 1000, "-" + mark,
                     markerfacecolor=face_c, markersize=marker_s,
                     linewidth=line_w, markevery=markers_on)

        plt.title(names[index - 1], fontsize='x-small')
        plt.xticks(np.arange(0, 21, step=5), fontsize='x-small')
        plt.yticks(np.arange(0, 15, step=2), fontsize='x-small')
        plt.grid(True)
        index += 1
    # plt.figlegend(lines, ('label1', 'label2', 'label3'), 'upper right')
    leg = fig.legend([],  # The line objects
               labels=['4', '7 ', '10'],  # The labels for each line
               loc="upper right",  # Position of legend
               borderaxespad=0.01,  # Small spacing around legend box
               fontsize='xx-small',
               # frameon=False,
               bbox_to_anchor=(0.99, 0.433),
                title = "#servers"
               )
    plt.setp(leg.get_title(), fontsize='xx-small')
    fig.text(0.49, 0.27, "Channels", ha="center", va="center", fontsize='small')
    fig.text(0.02, 0.5, "Throughput (KTxs/sec)", ha="center", va="center", rotation=90, fontsize='small')
    fig.tight_layout(rect=[0.015, 0, 0.94, 1])
    for d in oPath:
        plt.savefig(d + '/GD_throughput.pdf', bbox_inches='tight', pad_inches=0.08)
        plt.savefig(d + '/GD_throughput', bbox_inches = 'tight', pad_inches = 0.08)

if __name__ == "__main__":
    plotGDTrhoughput('/home/yoni/toy/gd', ["/home/yoni/toy/figures", "/home/yoni/Dropbox/paper/draws"])