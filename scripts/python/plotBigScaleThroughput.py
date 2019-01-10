from __future__ import division
import glob
import pandas as pd
import matplotlib.pyplot as plt
import numpy as np

from utiles import csvs2df
fs=11
line_w=1
marker_s=4
face_c='none'
markers=['s', 'x', '+', '^']

def drawBSThroghuputCharts(dir, oPath):
    evSize = ['100', '1000']
    m = 0
    fig = plt.figure(1, figsize=(3, 2))
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
    plt.xticks(np.arange(0, 21, step=5), fontsize=fs)
    plt.yticks(np.arange(0, 41, step=5), fontsize=fs)
    plt.grid(True)
    # index += 1
    # plt.figlegend(lines, ('label1', 'label2', 'label3'), 'upper right')
    leg = fig.legend([],  # The line objects
               labels=['$\\beta=100$', '$\\beta=1000$'],  # The labels for each line
                 loc="lower center",  # Position of legend
                 # borderaxespad=0.01,  # Small spacing around legend box
                 fontsize=fs,
                 frameon=False,
                 ncol=2,
                 bbox_to_anchor=(0.5, -0.04),
               )
    plt.setp(leg.get_title(), fontsize='small')
    fig.text(0.51, 0.1, "$\\omega$", ha="center", va="center", fontsize=fs)
    fig.text(0.02, 0.6, "Throughput ($\\frac{KTx}{sec}$)", ha="center", va="center", rotation=90, fontsize=fs)
    fig.tight_layout(rect=[0.015, 0.07, 1, 1])
    for d in oPath:
        plt.savefig(d + '/BS_throughput2.pdf', bbox_inches='tight', pad_inches=0.08)
        plt.savefig(d + '/BS_throughput2', bbox_inches = 'tight', pad_inches = 0.08)

if __name__ == "__main__":
    drawBSThroghuputCharts('/home/yoni/toy/res_49',
                           ["/home/yoni/toy/figures", "/home/yoni/Dropbox/paper/draws"])