SET QUOTED_IDENTIFIER ON
GO
SET ANSI_NULLS ON
GO
CREATE TABLE [dbo].[testtable2](
    [field1] [int] NULL,
    [field2] [int] NULL,
    [field3] [int] NOT NULL CONSTRAINT [cd3] DEFAULT ((7))
) ON [PRIMARY]
GO

