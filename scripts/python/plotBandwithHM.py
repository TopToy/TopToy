from __future__ import division
import glob
import pandas as pd
import matplotlib.pyplot as plt
import numpy as np
from matplotlib import cm
from matplotlib.axes import Axes

from utiles import csvs2df

fs=11

line_w=1
marker_s=4
face_c='none'
markers=['s', 'x', '+', '^']

def plotBandwidth(data, oPath):
    df = pd.read_csv(data, sep=",")
    # normData=
    regions = ['Ohaio', 'N. California', 'Singapore', 'Sau-Paulo', 'Paris', 'London', 'Central', 'Sydney', 'Oregon', 'N.Virgina']
    regions2 = ['Ohaio', 'N.Cali-\nfornia', 'Sing-\napore', 'Sau-\nPaulo', 'Paris', 'London', 'Central', 'Sydney', 'Oregon',
               'N.-\nVirgina']

    fig, ax = plt.subplots()

    cmap = cm.get_cmap('Reds')  # jet doesn't have white color
    cmap.set_bad('w')
    cmap.reversed()

    matrix_latency = df.as_matrix()
    mask = np.tri(matrix_latency.shape[0], k=-1)
    matrix_latency = np.ma.array(matrix_latency, mask=mask)
    matrix_latency = matrix_latency


    pa = ax.imshow(matrix_latency, cmap=cmap)
    df2 = pd.read_csv(data, sep=",")
    matrix_th = df2.as_matrix()
    mask = np.tri(matrix_th.shape[1], k=0)
    matrix_th = np.ma.array(matrix_th.transpose() , mask=mask).transpose()

    cmap = cm.get_cmap('Blues')  # jet doesn't have white color
    cmap.set_bad(alpha = 0.0)
    pb = ax.imshow(matrix_th, interpolation="nearest", cmap=cmap)

    #
    ax.set_xticks(np.arange(10))
    ax.set_yticks(np.arange(10))
    #
    ax.set_xticklabels(regions2, fontsize=10)
    ax.set_yticklabels(regions, fontsize=10)
    #
    ax.set_xticks(np.arange(10 + 1) - .5, minor=True)
    ax.set_yticks(np.arange(10 + 1) - .5, minor=True)


    for edge, spine in ax.spines.items():
        spine.set_visible(False)
    #
    ax.grid(which="minor", color="black", linestyle='-', linewidth=3)
    ax.tick_params(which="minor", bottom=False, left=False)
    # plt.setp(ax.get_xticklabels(), rotation=45, ha="right",
    #          rotation_mode="anchor")
    matrix = df.as_matrix()
    for i in range(10):
        for j in range(10):
            clr = 'black'
            if (i < j):
                if round(matrix[i][j], 2) > 100:
                    clr='white'
                text = ax.text(j, i, str(round(matrix[i][j], 2)),
                               ha="center", va="center", color=clr, fontsize=fs)
                continue
            if (i > j):
                if round(matrix[i][j], 2) >= 20:
                    clr='white'
                text = ax.text(j, i, str(round(matrix[i][j], 2)),
                           ha="center", va="center", color=clr, fontsize=fs)
                continue
            # if (i == j):
            #     text = ax.text(j, i, "X",
            #                    ha="center", va="center", color=clr, fontsize=fs)
    # ax.set_xlim(-1, 10)
    # ax.set_ylim(0, 10)
    # plt.axis((10, 10, 10, 10))
    fig.tight_layout(rect=[0, -0.3, 1, 1])
    for d in oPath:
        plt.savefig(d + '/heatmap_bw.pdf', bbox_inches='tight')
        plt.savefig(d + '/heatmap_bw', bbox_inches='tight')


if __name__ == "__main__":
    plotBandwidth("/home/yoni/toy/hm.csv",  ["/home/yoni/toy/figures", "/home/yoni/Dropbox/paper/draws"])