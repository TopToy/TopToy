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

def drawBSThroghuputCharts(dir, oPath):
    evSize = ['100', '1000']
    m = 0
    fig = plt.figure(1)
    for e in evSize:
        files = glob.glob(dir + "*/*." + e + "/servers/res/summery.csv")
        mark = markers[m]
        m += 1
        df = csvs2df(files)
        df = df[['channels', 'txPsec']][(df.channels == 1) | (df.channels % 2 == 0)].groupby(df.channels).mean()
        markers_on = [0, 3, 6, 10]
        plt.plot(df['channels'], df['txPsec'] / 1000, "-" + mark,
                 markerfacecolor=face_c, markersize=marker_s,
                 linewidth=line_w, markevery=markers_on)

    # plt.title(names[index - 1], fontsize='x-small')
    plt.xticks(np.arange(0, 21, step=5), fontsize='x-small')
    plt.yticks(np.arange(0, 46, step=5), fontsize='x-small')
    plt.grid(True)
    # index += 1
    # plt.figlegend(lines, ('label1', 'label2', 'label3'), 'upper right')
    leg = fig.legend([],  # The line objects
               labels=['100', '1000'],  # The labels for each line
               loc="upper right",  # Position of legend
               borderaxespad=0.01,  # Small spacing around legend box
               fontsize='xx-small',
               # frameon=False,
               bbox_to_anchor=(0.99, 0.98),
                title = "Txs/block"
               )
    plt.setp(leg.get_title(), fontsize='xx-small')
    fig.text(0.47, 0.01, "Channels", ha="center", va="center", fontsize='small')
    fig.text(0.02, 0.5, "Throughput (KTxs/sec)", ha="center", va="center", rotation=90, fontsize='small')
    fig.tight_layout(rect=[0.015, 0, 0.93, 1])
    for d in oPath:
        plt.savefig(d + '/BS_throughput2.pdf', bbox_inches='tight', pad_inches=0.08)
        plt.savefig(d + '/BS_throughput2', bbox_inches = 'tight', pad_inches = 0.08)

if __name__ == "__main__":
    drawBSThroghuputCharts('/home/yoni/toy/res_49',
                           ["/home/yoni/toy/figures", "/home/yoni/Dropbox/paper/draws"])