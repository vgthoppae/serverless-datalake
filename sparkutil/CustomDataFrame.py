class CustomDataFrame:
  def __init__(self, df):
    self._df = df

  def doesContain(self, colName, val):
    if (self._df[self._df[colName].isin(val)].count() > 0):
      return True
    else:
      return False
