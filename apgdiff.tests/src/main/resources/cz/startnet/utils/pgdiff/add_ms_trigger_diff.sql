SET QUOTED_IDENTIFIER ON
GO
SET ANSI_NULLS ON
GO
CREATE TRIGGER [dbo].[trigger1] ON [dbo].[table1]  
AFTER INSERT, UPDATE   
AS RAISERROR ('Notify!', 16, 10)
GO