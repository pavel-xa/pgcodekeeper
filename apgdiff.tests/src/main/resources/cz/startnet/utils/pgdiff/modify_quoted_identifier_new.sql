SET QUOTED_IDENTIFIER OFF
GO
SET ANSI_NULLS ON
GO
CREATE TABLE [dbo].[t1](
    [c1] [bigint] NOT NULL
) ON [PRIMARY]
GO

SET QUOTED_IDENTIFIER OFF
GO
SET ANSI_NULLS ON
GO
CREATE FUNCTION [dbo].[f1](@first int, @second int)
RETURNS real
AS
BEGIN
    RETURN 1.0;
END
GO

SET QUOTED_IDENTIFIER OFF
GO
SET ANSI_NULLS ON
GO
CREATE PROCEDURE [dbo].[p1]
AS
    select 1;
GO

SET QUOTED_IDENTIFIER OFF
GO
SET ANSI_NULLS ON
GO
CREATE VIEW [dbo].[v1] as
    SELECT 1 x;
GO

SET QUOTED_IDENTIFIER OFF
GO
SET ANSI_NULLS ON
GO
CREATE TRIGGER [dbo].[tr1] ON [dbo].[t1]
AFTER INSERT, UPDATE, DELETE
AS
   EXEC [dbo].[p1];
GO